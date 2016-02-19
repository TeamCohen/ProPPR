"""
Many portions adapted from the theano MNIST logistic regression tutorial:
    http://deeplearning.net/tutorial/logreg.html
"""
__docformat__ = 'restructedtext en'

import cPickle
import gzip
import os
import shutil
import sys
import timeit
import numpy
import theano
import theano.tensor as T
import logging
logging.basicConfig(level=logging.DEBUG)

THEANO_PREFIX = "theano_p("
N_THEANO_PREFIX = len(THEANO_PREFIX)
# def getTheanoFeatures(featureFile):
#     ret = {}
#     iret = {}
#     with open(featureFile,'r') as f:
#         i=0
#         for line in f:
#             i+=1
#             line = line.strip()
#             x = line.find("\t")
#             if x>0: line = line[:x]
#             if line.startswith(THEANO_PREFIX):
#                 ret[line] = i
#                 iret[i] = line
#     return (ret,iret)

def handleProPPRFeature(feature,tf):
    if feature.startswith(THEANO_PREFIX):
        tf[feature] = len(tf)
        return True
    return False

def featureToArgs(f) :
    return f[N_THEANO_PREFIX:f.index(")")].split(",")


class InstanceData(object):
    def __init__(self,filename):
        logging.info( "Loading vectors from %s..." % filename )
        with open(filename,'r') as f:
            self.index = cPickle.load(f)
            self.vectors = cPickle.load(f)

class Model(object):
    """
    This class encodes the theano model, along with model-specific
    utilities for loading vectors from the theano db, converting ProPPR
    features to their appropriate theano vector, &c.

    Typical use:

     1. m = Model(paramsFile) # Instantiate
     2. m.load_data(theanodbfile, propprgradientfile)
     3. m.ready() # creates/loads model parameters according to training data
     4. fun = m.trainingFunction()
     5. fun(minibatch_index) # train 1 epoch
     6. m.save()
     
     Base class defines the following fields:
     

     paramsFile - filename for theano model parameters
     tf - maps feature string -> matrix row #
     db - holds theano InstanceData
     M - total #examples
     batch_size - minibatch #examples
     dldf - symbolic variable for partial gradient
     data_dldf - numeric data for partial gradient
     has_queryData - true if M,tf,db are loaded
     has_trainingData - true if data_dldf is loaded

    """
    def __init__(self,filename,batch_size=-1):
        self.has_trainingData = False
        self.has_queryData = False
        self.paramsFile=filename
        self.dldf = T.dvector('dldf')
        self.batch_size = batch_size
        self._init()
    def ready(self):
        if not self.has_queryData: assert "load data first"
        if os.path.isfile(self.paramsFile):
            self._load(self.paramsFile)
        else:
            self._fresh()
        if self.batch_size<0: self.batch_size = self.M
    def load_queryData(self,dbfile,featureFile):
        self.tf = {}
        logging.info("Loading features from %s..." % featureFile)
        with open(featureFile,'r') as f:
            ln = 1
            for line in f:
                line=line.strip()
                if line[0] == "#": continue
                x = line.find("\t")
                if x>0: line = line[:x]
                handleProPPRFeature(line,self.tf)
        self.M = len(self.tf)
        logging.debug("Features detected: %d" % len(self.tf))
        self.db = InstanceData(dbfile)
        self._init_queryData()
        for feature,fid in self.tf.iteritems():
            self._build_queryData(fid,featureToArgs(feature))
        self.has_queryData = True
    def load_trainingData(self,dbfile,gradientFile):
        self.load_queryData(dbfile,gradientFile)
        self._init_trainingData()
        logging.info("Loading gradient from %s..." % gradientFile)
        with open(gradientFile,'r') as f:
            for line in f:
                line=line.strip()
                if line[0] == "#": continue
                (feature,wt) = line.split("\t")
                if feature not in self.tf: continue
                self.data_dldf.itemset(self.tf[feature],float(wt))
        self.has_trainingData = True
    def _init_queryData(self):
        pass
    def _init_trainingData(self):
        logging.debug("initialized Model data")
        self.data_dldf = numpy.zeros(self.M,dtype=theano.config.floatX)
    def trainingFunction(self,index):
        if not self.has_trainingData: assert "load data first"
        return theano.function(
            inputs=[index],
            outputs=self._outputs(),
            updates=self._updates(),
            givens=self._givens(index),
            on_unused_input='ignore'
            )
    def scoringFunction(self,index):
        if not self.has_queryData: assert "load data first"
        return theano.function(
            inputs=[index],
            outputs=self._scores(),
            givens=self._givens(index),
            on_unused_input='ignore'
            )
    def featureIndex(self):
        if not self.has_queryData: assert "load data first"
        return self.tf
    def _load(self,filename):
        # this may go poorly?
        raise NotImplementedError()
    def _init(self):
        pass
    def _fresh(self):
        pass
    def save(self,filename):
        raise NotImplementedError()
    def _build_queryData(self,i,args):
        pass
    def _build_trainingData(self,i,args):
        pass
    def _outputs(self):
        raise NotImplementedError()
    def _scores(self):
        raise NotImplementedError()
    def _updates(self):
        raise NotImplementedError()
    def _givens(self,index):
        raise NotImplementedError()


class LogisticRegression(Model):
    """Multi-class Logistic Regression Class

    The logistic regression is fully described by a weight matrix :math:`W`
    and bias vector :math:`b`. Classification is done by projecting data
    points onto a set of hyperplanes, the distance to which is used to
    determine a class membership probability.
    
    Adapted from the theano MNIST logistic regression tutorial:
    http://deeplearning.net/tutorial/logreg.html
    """
    def _name(self):
        return "LogisticRegression"
    def _init(self):
        super(LogisticRegression,self)._init()
        self.x = T.matrix('x')
        self.y = T.ivector('y')
        self.learning_rate = 0.13
    def _load(self,filename):
        with open(filename,'r') as f:
            clazz = cPickle.load(f)
            if clazz != self._name(): assert "Saved modelType %s incompatible with current modelType %s" % (clazz,self._name())
            self.W = cPickle.load(f)
            self.b = cPickle.load(f)
        self._setup()
    def _fresh(self):
        # initialize with 0 the weights W as a matrix of shape (vectorsize, #labels)
        self.W = theano.shared(
            value=numpy.zeros(
                (self.N, len(self.yindex)),
                dtype=theano.config.floatX
            ),
            name='W',
            borrow=True
        )
        # initialize the biases b as a vector of n_out 0s
        self.b = theano.shared(
            value=numpy.zeros(
                (len(self.yindex),),
                dtype=theano.config.floatX
            ),
            name='b',
            borrow=True
        )
        self._setup()
    def _setup(self):
        # symbolic expression for computing the matrix of class-membership
        # probabilities
        # Where:
        # W is a matrix where column-k represent the separation hyperplane for
        # class-k
        # x is a matrix where row-j  represents input training sample-j
        # b is a vector where element-k represent the free parameter of
        # hyperplane-k
        self.p_y_given_x = T.nnet.softmax(T.dot(self.x, self.W) + self.b)

        # symbolic description of how to compute prediction as class whose
        # probability is maximal
        self.y_pred = T.argmax(self.p_y_given_x, axis=1)

        # parameters of the model
        self.params = [self.W, self.b]

        # the cost we minimize during training is 
        # dL/dz = dL/df * df/dz
        # dL/df comes from ProPPR; the f in question tells us which x and y to use
        # in computing the negative log likelihood of
        # the model in symbolic format        
        self.cost = T.mean( -self.dldf * self.negative_log_likelihood() )

        g_W = T.grad(cost=self.cost, wrt=self.W)
        g_b = T.grad(cost=self.cost, wrt=self.b)

        # specify how to update the parameters of the model as a list of
        # (variable, update expression) pairs.
        self.updates = [(self.W, self.W - self.learning_rate * g_W),
                   (self.b, self.b - self.learning_rate * g_b)]
    def save(self):
        with open(self.paramsFile,'w') as f:
            cPickle.dump(self._name(),f)
            cPickle.dump(self.W,f)
            cPickle.dump(self.b,f)
    def _init_queryData(self):
        super(LogisticRegression,self)._init_queryData()
        logging.debug("initialized LogisticRegression training data")
        self.N=self.db.vectors.shape[1]
        self.data_trainX = numpy.zeros( 
            (self.M,self.N),
            dtype=theano.config.floatX )
        self.data_trainY = numpy.zeros( 
            self.M,
            dtype='int32' )
        self.yindex = {}
    def _build_queryData(self,i,args):
        (x,y) = args
        self.data_trainX[i,:] = self.db.vectors[self.db.index[x],:]
        if y not in self.yindex: self.yindex[y] = len(self.yindex)
        self.data_trainY[i] = self.yindex[y]
    def _outputs(self):
        return [
            -(
                T.dot(-self.dldf, T.neq(self.y_pred,self.y)) + 
                T.dot( self.dldf, T.eq( self.y_pred,self.y))
                )
             ]
    def _scores(self):
        return [self.p_y_given_x[T.arange(self.y.shape[0]),self.y]]
    def _updates(self):
        return self.updates
    def _givens(self,index):
        begin = index * self.batch_size
        end = (index+1) * self.batch_size
        yshared = T.cast(
                theano.shared(
                    numpy.asarray(
                        self.data_trainY,
                        dtype=theano.config.floatX),
                    borrow=True),
                'int32')[begin:end]
        xshared = theano.shared(
                numpy.asarray(
                    self.data_trainX,
                    dtype=theano.config.floatX),
                borrow=True)[begin:end]
        ret = {
            self.y:yshared,
            self.x:xshared,
            }
        if self.has_trainingData:
            dshared = theano.shared(
                numpy.asarray(
                    self.data_dldf,
                    dtype=theano.config.floatX),
                borrow=True)[begin:end]
            ret[self.dldf] = dshared
        return ret
            
    ######################
    def negative_log_likelihood(self):
        """Return the negative log-likelihood of the prediction
        of this model under the target distribution.

        .. math::

            \frac{1}{|\mathcal{D}|} \mathcal{L} (\theta=\{W,b\}, \mathcal{D}) =
            \frac{1}{|\mathcal{D}|} \sum_{i=0}^{|\mathcal{D}|}
                \log(P(Y=y^{(i)}|x^{(i)}, W,b)) \\
            \ell (\theta=\{W,b\}, \mathcal{D})

        """
        # y.shape[0] is (symbolically) the number of rows in y, i.e.,
        # number of examples (call it n) in the minibatch
        # T.arange(y.shape[0]) is a symbolic vector which will contain
        # [0,1,2,... n-1] T.log(self.p_y_given_x) is a matrix of
        # Log-Probabilities (call it LP) with one row per example and
        # one column per class LP[T.arange(y.shape[0]),y] is a vector
        # v containing [LP[0,y[0]], LP[1,y[1]], LP[2,y[2]], ...,
        # LP[n-1,y[n-1]]] and T.mean(LP[T.arange(y.shape[0]),y]) is
        # the mean (across minibatch examples) of the elements in v,
        # i.e., the mean log-likelihood across the minibatch.
        return -T.log(self.p_y_given_x)[T.arange(self.y.shape[0]), self.y]


class SimilarityRegression(LogisticRegression):
    def _name(self):
        return "SimilarityRegression"
    def _init(self):
        super(SimilarityRegression, self)._init()
        ox = T.matrix('ox')
        self.ox = ox
        self.x = T.prod(
            T.reshape(
                ox,
                (ox.shape[0],2,n_in/2)
                ),
            axis=1)
    def _init_queryData(self):
        super(LogisticRegression,self)._init_queryData() # skip LogisticRegression inputs
        logging.debug("initialized SimilarityRegression training data")
        self.N=self.db.vectors.shape[1]
        self.data_trainX = numpy.zeros( 
            (self.M,self.N*2),
            dtype=theano.config.floatX )
        self.data_trainY = numpy.zeros( self.M,dtype='int32' )
        self.yindex = {}
    def _build_queryData(self,i,args):
        (x1,x2) = args
        self.data_trainX[i,:self.N] = self.db.vectors[self.db.index[x1],:]
        self.data_trainX[i,self.N:] = self.db.vectors[self.db.index[x2],:]
        self.data_trainY[i] = 0
    # more about givens vs inputs in theano:
    # http://stackoverflow.com/questions/26879157/purpose-of-given-variables-in
    def _givens(self,index):
        foo = LogisticRegression._givens(self,index)
        foo[self.ox]=foo[self.x]
        del foo[self.x]
        return foo

class Pronghorn(object):
    def __init__(self,modelType):
        self.modelType = modelType
    def update(self,theanoModel,db_file,dldf_file):
        """
        adapted from logicstic_sgd.py:sgd_optimization_mnist()
        """
        logging.info( 'Loading the model...')
        # construct the logistic regression class
        self.classifier = self.modelType(theanoModel)
        self.classifier.load_trainingData(db_file,dldf_file)
        self.classifier.ready()

        # generate symbolic variables for input (minibatch number)
        index = T.lscalar()
        train_model = self.classifier.trainingFunction(index)

        logging.info( "Training..." )
        fitness = train_model(0)
        logging.debug( "model fitness: %s" % fitness)
        # save this best model
        self.classifier.save()
        return self.classifier
    def updateAndScore(self,theanoModel,db_file,dldf_file):
        self.update(theanoModel,db_file,dldf_file)
        return (self.classifier,self._score())
    def score(self,theanoModel,db_file,feat_file):
        logging.info( 'Loading the model...')
        # construct the logistic regression class
        self.classifier = self.modelType(theanoModel)
        self.classifier.load_queryData(db_file,feat_file)
        self.classifier.ready()
        
        return (self.classifier,self._score())
    def _score(self):
        # generate symbolic variables for input (minibatch number)
        index = T.lscalar()
        score_model = self.classifier.scoringFunction(index)
        logging.info("Scoring...")
        return score_model(0)[0]

def makebackup(f):
    bi=1
    backup = "%s.%d" % (f,bi)
    #backup_parent = "./"
    #if f[0] == "/": backup_parent=""
    #if f.rfind("/") > 0: backup_parent += f[:f.rfind("/")]
    while os.path.isfile(backup):#backup in os.listdir(backup_parent):
        bi+=1
        backup = "%s.%d" % (f,bi)
    return backup

def updateParamsFile(paramsFile,tfindex,scores):
    if os.path.isfile(paramsFile):
        logging.info( "Updating feature weights in params file %s..." % paramsFile )
        backup = makebackup(paramsFile)
        shutil.copyfile(paramsFile,backup)
        tfhit = {}
        with open(backup,'rb') as r, open(paramsFile,'wb') as w:
            ntotalmod=0
            # for each parameter
            for line in r:
                line=line.strip()
                if line[0]=="#": 
                    w.write(line)
                    w.write("\n")
                    continue
                (p,d,v)=line.partition("\t")
                if p not in tfindex:
                    w.write(line)
                    w.write("\n")
                    continue
                tfhit[p] = 1
                w.write(p)
                w.write(d)
                w.write("%g" % scores[tfindex[p]])
                w.write("\n")
                ntotalmod+=1
            for p,i in tfindex.iteritems():
                if p in tfhit: continue
                w.write(p)
                w.write("\t")
                w.write("%g" % scores[i])
                w.write("\n")
                ntotalmod+=1
            logging.debug( "\n%d total modifications" % ntotalmod )
    else:
        logging.info( "Writing feature weight to params file %s..." % paramsFile )
        with open(paramsFile,'wb') as w:
            for p,i in tfindex.iteritems():
                w.write(p)
                w.write("\t")
                w.write("%g" % scores[i])
                w.write("\n")
            logging.debug( "\n%d total features" % len(tfindex) )

################### command-line interface: ################

helpText = {}
def doUpdate():
    (grad,proppr,db,model) = sys.argv[2:6]
    if len(sys.argv) > 6:
        clazz=eval(sys.argv[6])
    else:
        clazz=LogisticRegression
    p = Pronghorn(clazz)
    (c,s) = p.updateAndScore(model,db,grad)
    updateParamsFile(proppr,c.featureIndex(),s)
helpText['update'] = ("dataset.gradient dataset.params dataset.pkl model.pkl [modelType]",
                      "Run 1 epoch of gradient descent from ProPPR partial gradient and update relevant feature weights in a ProPPR params file.")

def doQuery():
    (feat,proppr,db,model) = sys.argv[2:6]
    if len(sys.argv) > 6:
        clazz=eval(sys.argv[6])
    else:
        with open(model,'r') as f:
            clazz = eval(cPickle.load(f))
    p = Pronghorn(clazz)
    (c,s) = p.score(model,db,feat)
    updateParamsFile(proppr,c.featureIndex(),s)
helpText['query'] = ("dataset.features dataset.params dataset.pkl model.pkl [modelType]",
                     "Use a saved theano model to compute feature weights and save/update results in a ProPPR params file.")

def doHelp():
    print "Usage:"
    for cmd,(syntax,desc) in helpText.iteritems():
        print "\n    $ python %s %s %s" % (sys.argv[0],cmd,syntax)
        print "    %s" % desc
    print "\nModel types:"
    print "    LogisticRegression"
    print "    SimilarityRegression"

helpText['help'] = ("","This message.")

cmds = {
    "update":doUpdate,
    "query":doQuery,
    "help":doHelp
}

if __name__=="__main__":
    cmd="help"
    if len(sys.argv) > 1:
        cmd = sys.argv[1]
    if cmd not in cmds:
        print "Didn't recognized command '%s'." % cmd
        cmd="help"
    cmds[cmd]()
