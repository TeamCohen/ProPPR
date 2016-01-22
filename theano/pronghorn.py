"""
This tutorial introduces logistic regression using Theano and stochastic
gradient descent.

Logistic regression is a probabilistic, linear classifier. It is parametrized
by a weight matrix :math:`W` and a bias vector :math:`b`. Classification is
done by projecting data points onto a set of hyperplanes, the distance to
which is used to determine a class membership probability.

Mathematically, this can be written as:

.. math::
  P(Y=i|x, W,b) &= softmax_i(W x + b) \\
                &= \frac {e^{W_i x + b_i}} {\sum_j e^{W_j x + b_j}}


The output of the model or prediction is then done by taking the argmax of
the vector whose i'th element is P(Y=i|x).

.. math::

  y_{pred} = argmax_i P(Y=i|x,W,b)


This tutorial presents a stochastic gradient descent optimization method
suitable for large datasets.


References:

    - textbooks: "Pattern Recognition and Machine Learning" -
                 Christopher M. Bishop, section 4.3.2

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

class Model(object):
    def __init__(self, **kwargs):
        pass
    def negative_log_likelihood(self, y):
        pass
    def negative_log_likelihood_piecewise(self, y):
        pass
    def errors(self, y):
        pass


class LogisticRegression(Model):
    """Multi-class Logistic Regression Class

    The logistic regression is fully described by a weight matrix :math:`W`
    and bias vector :math:`b`. Classification is done by projecting data
    points onto a set of hyperplanes, the distance to which is used to
    determine a class membership probability.
    
    Adapted from the theano MNIST logistic regression tutorial:
    http://deeplearning.net/tutorial/logreg.html
    """

    def __init__(self, input_data, n_in, n_out, **kwargs):
        """ Initialize the parameters of the logistic regression

        :type input_data: theano.tensor.TensorType
        :param input_data: symbolic variable that describes the input of the
                      architecture (one minibatch)

        :type n_in: int
        :param n_in: number of input units, the dimension of the space in
                     which the datapoints lie

        :type n_out: int
        :param n_out: number of output units, the dimension of the space in
                      which the labels lie

        """
        # start-snippet-1
        # initialize with 0 the weights W as a matrix of shape (n_in, n_out)
        self.W = theano.shared(
            value=numpy.zeros(
                (n_in, n_out),
                dtype=theano.config.floatX
            ),
            name='W',
            borrow=True
        )
        # initialize the biases b as a vector of n_out 0s
        self.b = theano.shared(
            value=numpy.zeros(
                (n_out,),
                dtype=theano.config.floatX
            ),
            name='b',
            borrow=True
        )

        # symbolic expression for computing the matrix of class-membership
        # probabilities
        # Where:
        # W is a matrix where column-k represent the separation hyperplane for
        # class-k
        # x is a matrix where row-j  represents input training sample-j
        # b is a vector where element-k represent the free parameter of
        # hyperplane-k
        self.p_y_given_x = T.nnet.softmax(T.dot(input_data, self.W) + self.b)

        # symbolic description of how to compute prediction as class whose
        # probability is maximal
        self.y_pred = T.argmax(self.p_y_given_x, axis=1)
        # end-snippet-1

        # parameters of the model
        self.params = [self.W, self.b]

        # keep track of model input
        self.input_data = input_data

    def get_input(self):
        return self.input_data
    def negative_log_likelihood(self, y):
        """Return the mean of the negative log-likelihood of the prediction
        of this model under a given target distribution.

        .. math::

            \frac{1}{|\mathcal{D}|} \mathcal{L} (\theta=\{W,b\}, \mathcal{D}) =
            \frac{1}{|\mathcal{D}|} \sum_{i=0}^{|\mathcal{D}|}
                \log(P(Y=y^{(i)}|x^{(i)}, W,b)) \\
            \ell (\theta=\{W,b\}, \mathcal{D})

        :type y: theano.tensor.TensorType
        :param y: corresponds to a vector that gives for each example the
                  correct label

        Note: we use the mean instead of the sum so that
              the learning rate is less dependent on the batch size
        """
        # start-snippet-2
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
        return -T.mean(T.log(self.p_y_given_x)[T.arange(y.shape[0]), y])
        # end-snippet-2
    
    def negative_log_likelihood_piecewise(self, y):
        return -T.log(self.p_y_given_x)[T.arange(y.shape[0]), y]

    def errors(self, y):
        """Return a float representing the number of errors in the minibatch
        over the total number of examples of the minibatch ; zero one
        loss over the size of the minibatch

        :type y: theano.tensor.TensorType
        :param y: corresponds to a vector that gives for each example the
                  correct label
        """

        # check if y has same dimension of y_pred
        if y.ndim != self.y_pred.ndim:
            raise TypeError(
                'y should have the same shape as self.y_pred',
                ('y', y.type, 'y_pred', self.y_pred.type)
            )
        # check if y is of the correct datatype
        if y.dtype.startswith('int'):
            # the T.neq operator returns a vector of 0s and 1s, where 1
            # represents a mistake in prediction
            return T.mean(T.neq(self.y_pred, y))
        else:
            raise NotImplementedError()

class SimilarityRegression(Model):
    def __init__(self,input_data,n_in,**kwargs):
        """ Initialize the parameters of the model

        :type input: theano.tensor.TensorType
        :param input: symbolic variable that describes the input of the
                      architecture (one minibatch)

        :type n_in: int
        :param n_in: the length of an instance vector. instance vectors
                     for the similarity of ei and ej should be composed
                     as ei_0, ei_1, ..., ei_ni, ej_0, ej_1, ..., ej_nj
        """
        
        self.input_data = input_data
        self.inner_input = T.prod(
            T.reshape(
                input_data,
                (input_data.shape[0],2,n_in/2)
                ),
            axis=1)
        self.inner = LogisticRegression(self.inner_input,n_in/2,2)
        self.W = self.inner.W
        self.b = self.inner.b
        self.p_y_given_x = self.inner.p_y_given_x
        self.y_pred = self.inner.y_pred
    def get_input(self):
        return self.inner
    def negative_log_likelihood(self, y):
        return self.inner.negative_log_likelihood(y)
    def negative_log_likelihood_piecewise(self, y):
        return self.inner.negative_log_likelihood_piecewise(y)
    def errors(self, y):
        return self.inner.errors(y)

class Pronghorn(object):
    def __init__(self,model):
        self.model = model
    def update(self,theanoModel,dldf,train_set_x,train_set_y,learning_rate=0.13):
        """
        adapted from logicstic_sgd.py:sgd_optimization_mnist()
        """
        print '... building the model'

        # generate symbolic variables for input (x and y represent a
        # minibatch)
        x = T.matrix('x')  # data, presented as word vectors
        y = T.ivector('y')  # labels, presented as 1D vector of [int] labels

        # construct the logistic regression class

        classifier = []
        if os.path.isfile(theanoModel):
            # ...this might go poorly
            with open(theanoModel,'r') as f:
                classifier = cPickle.load(f)
        else:
            nclasses = len(set(train_set_y))
            classifier = self.model(**{'input_data':x, 'n_in':train_set_x.shape[1], 'n_out':nclasses})

        # the cost we minimize during training is 
        # dL/dz = dL/df * df/dz
        # dL/df comes from ProPPR; the f in question tells us which x and y to use
        # in computing the negative log likelihood of
        # the model in symbolic format
        logging.debug("dldf: %s\n%s" % (str(dldf.shape),dldf[:10]))
        cost = T.mean( dldf * classifier.negative_log_likelihood_piecewise(y) )

        # compute the gradient of cost with respect to theta = (W,b)
        g_W = T.grad(cost=cost, wrt=classifier.W)
        g_b = T.grad(cost=cost, wrt=classifier.b)

        # start-snippet-3
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
        foo = theano.function(
            inputs=[classifier.input_data],
            outputs=[g_W,g_b,classifier.W,classifier.b,classifier.p_y_given_x,classifier.get_input(),classifier.y_pred],
            givens={
                y: train_set_y
            },
            on_unused_input='ignore'
        )

        # update W,b
        firstSix = (0,1,2,3,4,5,6,300,301,302,303,304,305,306)
        #logging.debug( "train_set_x: %s\n%s" % (str(train_set_x.shape),train_set_x[:3,firstSix]) )
        (foogW,foogb,fooW,foob,foop,fooi,fooy) = foo(train_set_x)
        logging.debug( "g_W, pre: %s\n%s" % (str(foogW.shape),foogW[:10]))
        logging.debug( "g_b, pre: %s\n%s" % (str(foogb.shape),foogb[:10]))
        logging.debug( "p,   pre: %s\n%s" % (str(foop.shape),foop[:10]))
        logging.debug( "yp,  pre: %s\n%s" % (str(fooy.shape),fooy[:10]))
        #logging.debug( "i,   pre: %s\n%s" % (str(fooi.shape),fooi[:3,:10]))
        logging.debug( "err, pre: %s" % fooi)
        logging.debug( "y,   pre: %s\n%s" % (str(train_set_y.shape),train_set_y[:10]))
        #logging.debug( "gW,  pre: %s\n%s" % (str(foob.shape),foob[:10]))
        #logging.debug( "inner input,  pre: %s\n%s" % (str(foob.shape),foob[:3,0:7]))
        avg_cost = train_model(train_set_x)
        (foogW,foogb,fooW,foob,foop,fooi,fooy) = foo(train_set_x)
        logging.debug( "W, post: %s\n%s" % (str(fooW.shape),fooW[:10]))
        logging.debug( "b, post: %s\n%s" % (str(foob.shape),foob[:10]))
        logging.debug( "p, post: %s\n%s" % (str(foop.shape),foop[:10]))
        logging.debug( "yp,post: %s\n%s" % (str(fooy.shape),fooy[:10]))
        logging.debug( "err,post: %s" % fooi)
        # save this best model
        with open(theanoModel, 'w') as f:
            cPickle.dump(classifier, f)
        return classifier

    def score(self, xs, ys, classifier):
        y = T.ivector('y')  # labels, presented as 1D vector of [int] labels
        score_model = theano.function(
            inputs=[y,classifier.input_data],
            outputs=classifier.negative_log_likelihood_piecewise(y)
        )
        return score_model(ys,xs)

    def xscore(self, xs, ys, classifier):
        y = T.ivector('y')  # labels, presented as 1D vector of [int] labels
        score_model = theano.function(
            inputs=[y,classifier.input_data],
            outputs=classifier.negative_log_likelihood_piecewise(y)
        )

        scores = []
        for el in ys:
            scores.append(score_model(el*numpy.ones(vectors.shape[0],dtype='int32'),
                                    xs))
        return scores


