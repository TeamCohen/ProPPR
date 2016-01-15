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
        logreg_input = T.prod(
            T.reshape(
                input_data,
                (input_data.shape[0],n_in/2,2)
                ),
            axis=2)
        logging.debug("shape of logistic regression input: " + str(logreg_input.shape))
        self.inner = LogisticRegression(logreg_input,n_in/2,1)
        self.W = self.inner.W
        self.b = self.inner.b
    def negative_log_likelihood(self, y):
        return self.inner.negative_log_likelihood(y)
    def negative_log_likelihood_piecewise(self, y):
        return self.inner.negative_log_likelihood_piecewise(y)
    def errors(self, y):
        return self.inner.errors(y)

class Pronghorn(object):
    def __init__(self,model):
        self.model = model
    def train(self,train_set_x,train_set_y,learning_rate=0.13, n_epochs=1000,
                               batch_size=600):
        """
        adapted from logicstic_sgd.py:sgd_optimization_mnist()
        """
        print '... building the model'

        # generate symbolic variables for input (x and y represent a
        # minibatch)
        x = T.matrix('x')  # data, presented as word vectors
        y = T.ivector('y')  # labels, presented as 1D vector of [int] labels

        # construct the logistic regression class
        # Each feature vector has 56 entries
        # there are 2 classes [TODO:DETECT]
        classifier = self.model(**{'input_data':x, 'n_in':train_set_x.shape[1], 'n_out':2})

        # the cost we minimize during training is the negative log likelihood of
        # the model in symbolic format
        cost = classifier.negative_log_likelihood(y)

        # compute the gradient of cost with respect to theta = (W,b)
        g_W = T.grad(cost=cost, wrt=classifier.W)
        g_b = T.grad(cost=cost, wrt=classifier.b)

        # start-snippet-3
        # specify how to update the parameters of the model as a list of
        # (variable, update expression) pairs.
        updates = [(classifier.W, classifier.W - learning_rate * g_W),
                   (classifier.b, classifier.b - learning_rate * g_b)]
        test_model = theano.function(
            inputs=[],
            outputs=classifier.errors(y),
            givens={
                x: train_set_x,
                y: train_set_y
            }
        )
        validate_model = test_model

        # compiling a Theano function `train_model` that returns the cost, but in
        # the same time updates the parameter of the model based on the rules
        # defined in `updates`
        train_model = theano.function(
            inputs=[],
            outputs=cost,
            updates=updates,
            givens={
                x: train_set_x,
                y: train_set_y
            }
        )
        # end-snippet-3
        ###############
        # TRAIN MODEL #
        ###############
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
                        # test it on the test set
                        test_score = test_model()

                        print(
                            (
                                '     epoch %i, test error of'
                                ' best model %f %%'
                            ) %
                            (
                                epoch,
                                test_score * 100.
                            )
                        )

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
                'with test performance %f %%'
            )
            % (best_validation_loss * 100., test_score * 100.)
        )
        print 'The code run for %d epochs, with %f epochs/sec' % (
            epoch, 1. * epoch / (end_time - start_time))
        print >> sys.stderr, ('The code for file ' +
                              os.path.split(__file__)[1] +
                              ' ran for %.1fs' % ((end_time - start_time)))
        return classifier

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
        logging.debug("dldf: %s\n%s" % (str(dldf.shape),dldf))
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
            outputs=[classifier.W,classifier.b],
            on_unused_input='ignore'
        )

        # update W,b
        logging.debug( "train_set_x: %s" % str(train_set_x.shape) )
        (fooW,foob) = foo(train_set_x)
        logging.debug( "W,  pre: %s\n%s" % (str(fooW.shape),fooW[:10]))
        logging.debug( "b,  pre: %s\n%s" % (str(foob.shape),foob[:10]))
        avg_cost = train_model(train_set_x)
        (fooW,foob) = foo(train_set_x)
        logging.debug( "W, post: %s\n%s" % (str(fooW.shape),fooW[:10]))
        logging.debug( "b, post: %s\n%s" % (str(foob.shape),foob[:10]))
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


