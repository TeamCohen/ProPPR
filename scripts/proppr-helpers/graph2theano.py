#!/usr/bin/python

import sys
import cPickle
import numpy
import theano

def graph2theano(infile,outfile):
    ids = {}
    next_id=0
    data = {}
    print "loading..."
    with open(infile,'r') as f:
        cache = []
        last_src = ""
        for line in f:
            parts = line.strip().split("\t")
            (pred,src,dst) = parts[0:3]
            if last_src != "" and src != last_src:
                data[last_src] = cache
                cache = []
            if dst not in ids: 
                ids[dst] = next_id
                next_id += 1
            cache.append(ids[dst])
            last_src = src
        data[last_src] = cache
    vectors = numpy.zeros(
        (len(data), len(ids)),
        dtype=theano.config.floatX
        )
    
    print "vector is %s" % str(vectors.shape)
    vectorIndex = {}
    i=0
    for src,dsts in data.iteritems():
        vectorIndex[src] = i
        for d in dsts:
            vectors.itemset(
                (i,d),
                1. #this is where the graph weight would go
                )
        i += 1
    print "saving..."
    with open(outfile,'w') as f:
        cPickle.dump(vectorIndex, f)
        cPickle.dump(vectors, f)
    print "done."

            
if __name__=='__main__':
    if len(sys.argv)<3:
        print "Usage:\n\t%s dataset.graph dataset.theano.pkl"
        print "NB: Weighted graphs not yet supported"
        exit(1)
    graph2theano(sys.argv[1],sys.argv[2])
