#!/usr/bin/python

import sys
import cPickle
import numpy
import theano

def embeddings2theano(infile,outfile):
    """ format:
    word [tab] f0 [tab] f1 [tab] ... [tab] fN
    """
    print "loading..."
    vectorIndex = {}
    i=0
    vectors = []
    vinit=True
    N=0
    with open(infile,'r') as f:
        for line in f:
            N +=1
    with open(infile,'r') as f:
        for line in f:
            word,emb = line.strip().split("\t",1)
            word = word.lower()
            vectorIndex[word] = i
            embv = [float(k) for k in emb.split()]
            if vinit:
                vinit=False
                vectors = numpy.zeros( (N,len(embv)),dtype=theano.config.floatX)
            vectors[i] = embv
            i += 1
    print "saving..."
    with open(outfile,'w') as f:
        cPickle.dump(vectorIndex,f)
        cPickle.dump(vectors,f)
    print "done."

if __name__ == '__main__':
    if len(sys.argv)<3:
        print "Usage:\n\t%s dataset.embeddings dataset.embeddings.pkl"
        exit(1)
    embeddings2theano(sys.argv[1],sys.argv[2])
