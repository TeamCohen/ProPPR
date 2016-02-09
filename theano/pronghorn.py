"""
Many portions adapted from the theano MNIST logistic regression tutorial:
    http://deeplearning.net/tutorial/logreg.html
"""
__docformat__ = 'restructedtext en'

import cPickle
import gzip
import os
import sys
import timeit
import numpy
import theano
import theano.tensor as T
import logging
logging.basicConfig(level=logging.DEBUG)

THEANO_PREFIX = "theano_p("
N_THEANO_PREFIX = len(THEANO_PREFIX)
def getTheanoFeatures(featureFile):
    ret = {}
    iret = {}
    with open(featureFile,'r') as f:
        i=0
        for line in f:
            i+=1
            line = line.strip()
            x = line.find("\t")
            if x>0: line = line[:x]
            if line.startswith(THEANO_PREFIX):
                ret[line] = i
                iret[i] = line
    return (ret,iret)

def featureToArgs(f) :
    return f[N_THEANO_PREFIX:f.index(")")].split(",")


class InstanceData(object):
    def __init(self,filename):
        print "Loading vectors from %s..." % filename
        with open(filename,'r') as f:
            self.index = cPickle.load(f)
            self.vectors = cPickle.load(f)

class Model(object):
    def __init__(self,filename=False):
        self.has_trainingData = False
        self.paramsFile=filename
        self.dldf = T.matrix('dldf')
    def ready(self):
        if not self.has_trainingData: assert "load training data first"
        if self.paramsFile:
            self.load(self.paramsFile)
        else:
            self.init()
    def load_trainingData(self,dbfile,featurefile,gradfile):
        print "Loading features from %s..." % featurefile
        (tf,itf) = getTheanoFeatures(featurefile)
        self.tf = tf
        self.M = len(tf)
        self.db = InstanceData(dbfile)
        self._init_trainingData()
        self.tfindex = {}
        i=0
        print "Loading gradient from %s..." % gradientFile
        with open(gradientFile,'r') as f:
            ln = 1
            for line in f:
                line=line.strip()
                if line[0] == "#": continue
                (feature,wt) = line.split("\t")
                if feature in self.tf:
                    tfindex[feature] = i
                    self.data_dldf.itemset(i,float(wt))
                    self._build_trainingData(i,featureToArgs(feature))
                    i+=1
        self.has_trainingData = True
    def _init_trainingData(self):
        self.data_dldf = numpy.zeros(self.M,dtype=theano.config.floatX)
    def trainingFunction(self,index):
        return theano.function(
            inputs=[index],
            outputs=self._outputs(),
            updates=self._updates(),
            givens=self._givens(index)
            )
    def _load(self,filename):
        # this may go poorly
        raise NotImplementedError()
    def _init(self):
        raise NotImplementedError()
    def save(self,filename):
        raise NotImplementedError()
    def _build_trainingData(self,i,args):
        raise NotImplementedError()
    def _outputs(self):
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
    
    def __init__(self,**kwargs):
        super(LogisticRegression,self).__init__(**kwargs)
        self.x = T.matrix('x')
        self.y = T.ivector('y')
    def _load(self,filename):
        with open(filename,'r') as f:
            self.W = cPickle.load(f)
            self.b = cPickle.load(f)
        self._setup()
    def _init(self):
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
        
        self.cost = T.mean( -self.dldf * self.negative_log_likelihood() )
        g_W = T.grad(cost=cost, wrt=self.W)
        g_b = T.grad(cost=cost, wrt=self.b)

        # specify how to update the parameters of the model as a list of
        # (variable, update expression) pairs.
        self.updates = [(self.W, self.W - learning_rate * g_W),
                   (self.b, self.b - learning_rate * g_b)]
    def save(self,filename):
        with open(filename,'w') as f:
            cPickle.save(self.W,f)
            cPickle.save(self.b,f)
    def _init_trainingData(self):
        super(LogisticRegression,self)._init_trainingData()
        self.N=self.db.vectors.shape[1]
        self.data_trainX = numpy.zeros( (self.M,N),dtype=theano.config.floatX )
        self.data_trainY = numpy.zeros( self.M,dtype='int32' )
        self.yindex = {}
    def _build_trainingData(self,i,args):
        (x,y) = args
        self.data_trainX[i,:] = self.db.vectors[self.db.vectorIndex[x],:]
        if y not in self.yindex: self.yindex[y] = len(self.yindex)
        self.data_trainY[i] = self.yindex[y]
    def _outputs(self):
        return [
            -(
                T.dot(-self.dldf, T.neq(self.y_pred,self.y)) + 
                T.dot( self.dldf, T.eq( self.y_pred,self.y))
                )
             ]
    def _updates(self):
        return self.updates
    def _givens(self,index):
        return {
            'y':self.data_trainY,
            'x':self.data_trainX
            }
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
        return -T.log(self.p_y_given_x)[T.arange(self.input_y.shape[0]), self.input_y]


class SimilarityRegression(LogisticRegression):
    def __init__(self, n_in,**kwargs):
        """ Initialize the parameters of the model

        :type x_data: theano.tensor.TensorType
        :param x_data: symbolic variable that describes the input of the
                      architecture (one minibatch)

        :type n_in: int
        :param n_in: the length of an instance vector. instance vectors
                     for the similarity of ei and ej should be composed
                     as ei_0, ei_1, ..., ei_ni, ej_0, ej_1, ..., ej_nj
        """
        super(SimilarityRegression, self).__init__(n_in/2,2)
        ox = T.matrix('ox')
        self.outer_input_x = ox
        self.inner_input_x = T.prod(
            T.reshape(
                self.outer_input_x,
                (self.outer_input_x.shape[0],2,n_in/2)
                ),
            axis=1)
        self.input_x = self.inner_input_x
    def getInput(self):
        ret = Model.getInput(self)
        ret.append(self.outer_input_x)
        return ret
    # def getGivens(self):
    #     ret = Model.getGivens(self)
    #     ret[self.input_y] = numpy.zeros(self.outer_input_x.shape[0],dtype='int32')
    #     return ret
"""
TODO: figure out how to use input vs given in theano.function.

Then clarify how givens & inputs lists should be filled, and who should fill them.

Re:
http://stackoverflow.com/questions/26879157/purpose-of-given-variables-in
"""
    def dataTemplate(self,size,datastore):
        """ Returns a tuple of empty input data vectors """
        ret = super(SimilarityRegression,self).dataTemplate(size,datastore)
        N=datastore.vectors.shape[1]
        trainX = numpy.zeros( (size,N*2),dtype=theano.config.floatX )
        return (trainX,)
    def fillDataForFeature(self,i,data,datastore,feature):
        """ Fills one row of model input data according to a ProPPR feature """
        N=datastore.vectors.shape[1]
        (x1,x2) = feature
        (trainX,) = data
        trainX[i,:N] = datastore.vectors[datastore.index[x1],:]
        trainX[i,N:] = datastore.vectors[datastore.index[x2],:]

class Pronghorn(object):
    def __init__(self,model):
        self.model = model
    def _setup(self,theanoModel,dldf,train_set_x,train_set_y,learning_rate=0.13):
        print '... building the model'

        # generate symbolic variables for input (x and y represent a
        # minibatch)
        
        # construct the logistic regression class

        classifier = []
        if os.path.isfile(theanoModel):
            # ...this might go poorly
            with open(theanoModel,'r') as f:
                classifier = cPickle.load(f)
        else:
            nclasses = len(set(train_set_y))
            classifier = self.model(**{'dldf':dldf, 'x_data':x, 'n_in':train_set_x.shape[1], 'n_out':nclasses})

        # the cost we minimize during training is 
        # dL/dz = dL/df * df/dz
        # dL/df comes from ProPPR; the f in question tells us which x and y to use
        # in computing the negative log likelihood of
        # the model in symbolic format
        logging.debug("dldf: %s\n%s" % (str(dldf.shape),dldf[:10]))
        cost = T.mean( -dldf * classifier.negative_log_likelihood_piecewise(y) )

        # compute the gradient of cost with respect to theta = (W,b)
        g_W = T.grad(cost=cost, wrt=classifier.W)
        g_b = T.grad(cost=cost, wrt=classifier.b)

        # specify how to update the parameters of the model as a list of
        # (variable, update expression) pairs.
        updates = [(classifier.W, classifier.W - learning_rate * g_W),
                   (classifier.b, classifier.b - learning_rate * g_b)]

        # compiling a Theano function `train_model` that returns the cost, but in
        # the same time updates the parameter of the model based on the rules
        # defined in `updates`
        train_model = theano.function(
            inputs=[classifier.input_data],
            outputs=cost,
            updates=updates,
            givens={
                y: train_set_y
            }
        )
        return (classifier,g_W,g_b,x,y,train_model)
    def update(self,theanoModel,dldf,train_set,learning_rate=0.13):
        """
        adapted from logicstic_sgd.py:sgd_optimization_mnist()
        """
        (classifier,g_W,g_b,x,y,train_model) = self._setup(theanoModel,dldf,train_set,learning_rate)

        def adjusted_errors(classifier,y,dldf):
            return T.dot(-dldf,T.neq(classifier.y_pred,y)) + T.dot(dldf,T.eq(classifier.y_pred,y))
        
        foo = theano.function(
            inputs=classifier.getInput(),
            outputs=[g_W,g_b,classifier.W,classifier.b,classifier.p_y_given_x,classifier.get_input(),classifier.y_pred,adjusted_errors(classifier,y,dldf)],
            givens=classifier.getGivens(),
            on_unused_input='ignore'
        )

        # update W,b
        firstSix = (0,1,2,3,4,5,6,300,301,302,303,304,305,306)
        #logging.debug( "train_set_x: %s\n%s" % (str(train_set_x.shape),train_set_x[:3,firstSix]) )
        (foogW,foogb,fooW,foob,foop,fooi,fooy,fooerr) = foo(train_set_x)
        #logging.debug( "g_W, pre: %s\n%s" % (str(foogW.shape),foogW[:10]))
        #logging.debug( "g_b, pre: %s\n%s" % (str(foogb.shape),foogb[:10]))
        #logging.debug( "W,   pre: %s\n%s" % (str(fooW.shape),fooW[:10]))
        logging.debug( "p,   pre: %s\n%s" % (str(foop.shape),foop[:10]))
        """
What we want here is for the yp to match y where dldf is <0, and
yp to neq y where dldf >0. This drops the overall loss (cueing off dldf)
"""

        logging.debug( "yp,  pre: %s\n%s" % (str(fooy.shape),fooy[:10]))
        #logging.debug( "i,   pre: %s\n%s" % (str(fooi.shape),fooi[:3,:10]))
        #logging.debug( "input, pre: %s" % fooi)
        logging.debug( "y,   pre: %s\n%s" % (str(train_set_y.shape),train_set_y[:10]))
        logging.debug( "loss pre: %s" % (fooerr))
        #logging.debug( "gW,  pre: %s\n%s" % (str(foob.shape),foob[:10]))
        #logging.debug( "inner input,  pre: %s\n%s" % (str(foob.shape),foob[:3,0:7]))
        avg_cost = train_model(train_set)
        (foogW,foogb,fooW,foob,foop,fooi,fooy,fooerr) = foo(train_set_x)
        #logging.debug( "W, post: %s\n%s" % (str(fooW.shape),fooW[:10]))
        #logging.debug( "b, post: %s\n%s" % (str(foob.shape),foob[:10]))
        logging.debug( "p, post: %s\n%s" % (str(foop.shape),foop[:10]))
        logging.debug( "yp,post: %s\n%s" % (str(fooy.shape),fooy[:10]))
        logging.debug( "loss,post: %s" % fooerr)
        # save this best model
        with open(theanoModel, 'w') as f:
            cPickle.dump(classifier, f)
        return classifier
    def train(self,theanoModel,dldf,train_set_x,train_set_y,learning_rate=0.13):
        validate_model = theano.function(
            inputs=[classifier.input_data],
            outputs=classifier.errors(y),
            givens={
                y: train_set_y
            }
        )
        (classifier,g_W,g_b,x,y,train_model) = self._setup(theanoModel,dldf,train_set_x,train_set_y,learning_rate)
        print '... training the model'
        # early-stopping parameters
        patience = 5  # look as this many examples regardless
        patience_increase = 2  # wait this much longer when a new best is
                                      # found
        improvement_threshold = 0.995  # a relative improvement of this much is
                                      # considered significant
        validation_frequency = 1 #patience / 2
        # go through this many
        # minibatche before checking the network
        # on the validation set
        # -- validate every epoch

        best_validation_loss = numpy.inf
        test_score = 0.
        start_time = timeit.default_timer()

        done_looping = False
        epoch = 0
        while (epoch < n_epochs) and (not done_looping):
            epoch = epoch + 1

            # update W,b
            avg_cost = train_model()
            # iteration number
            iter = (epoch - 1)

            if (iter + 1) % validation_frequency == 0:
                # compute zero-one loss on validation set
                this_validation_loss = validate_model()

                print(
                    'epoch %i, validation error %f %%' %
                    (
                        epoch,
                        this_validation_loss * 100.
                        )
                    )

                # if we got the best validation score until now
                if this_validation_loss < best_validation_loss:
                    #improve patience if loss improvement is good enough
                    if this_validation_loss < best_validation_loss *  \
                            improvement_threshold:
                        patience = max(patience, iter * patience_increase)

                        best_validation_loss = this_validation_loss

                        # save the best model
                        with open(theanoModel, 'w') as f:
                            cPickle.dump(classifier, f)

            if patience <= iter:
                done_looping = True
                break

        end_time = timeit.default_timer()
        print(
            (
                'Optimization complete with best validation score of %f %%,'
            )
            % (best_validation_loss * 100.)
        )
        print 'The code run for %d epochs, with %f epochs/sec' % (
            epoch, 1. * epoch / (end_time - start_time))
        print >> sys.stderr, ('The code for file ' +
                              os.path.split(__file__)[1] +
                              ' ran for %.1fs' % ((end_time - start_time)))
        return classifier

    def score(self, xs, ys, classifier):
        y = T.ivector('y')  # labels, presented as 1D vector of [int] labels
        score_model = theano.function(
            inputs=[y,classifier.input_data],
            outputs=classifier.p_y_given_x[T.arange(y.shape[0]),y]
        )
        return score_model(ys,xs)


