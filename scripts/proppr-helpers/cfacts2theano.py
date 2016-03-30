#!/usr/bin/python

import sys
import cPickle
import numpy
import theano
import subprocess as sub
from pipes import quote



def index(infile,args,suffix):
    outfile = infile+suffix
    cmd = 'cut -f %s %s | LC_ALL=C sort -u > %s' % (quote(args),quote(infile),quote(outfile))
    print cmd
    ret = sub.call(cmd,shell=True)
    if ret != 0:
        sys.exit(stat) #propagate failure
    return outfile

def loadIndex(indexFile,debug=False):
    ids = {}
    with open(indexFile,'r') as f:
        k=0
        for line in f:
            try:
                key = line.strip()
                if debug:
                    if k < 8:#in [68,6]: 
                        print ("%s:%d" % (indexFile,k),key,line)
                ids[key]=k
            except UnicodeError:
                print "Problem on line %d of %s:" % (k+1,indexFile)
                print line
                print key
                raise
            k += 1
    return ids

def cfacts2theano(infile,outfile,id_args="2",val_args="3",weighted=False,debug=False,*misc):
    if weighted: 
        weighted = bool(weighted)
        print "weighted: %s" % weighted
    if debug:
        debug = bool(debug)
        print "debug: %s" % debug
    
    print "building row/col indices..."
    idfile = index(infile,id_args,".i")
    valfile = index(infile,val_args,".v")
    
    print "loading from index..."
    ids = loadIndex(idfile)
    vals = loadIndex(valfile)#,debug=True)
    
    r = len(ids)
    c = len(vals)
    print "vector will be (%d,%d)" % (r,c)
    vectors = numpy.zeros(
        (r, c),
        dtype=theano.config.floatX
        )
    
    print "row ids are in fields",id_args
    print "col ids are in fields",val_args
    py_id_args = [int(x)-1 for x in id_args.split(",")]
    py_val_args = [int(x)-1 for x in val_args.split(",")]
    if debug:
        print ids
        print vals
    print "loading from %s..." % infile
    with open(infile,'r') as f:
        ln = 0
        for line in f:
            ln += 1
            parts = line.strip().split('\t')
            src = '\t'.join([parts[x] for x in py_id_args])
            if src not in ids:
                assert "Problematic row id: \"%s\" on line %d of %s" % (src,ln,infile)
            srckey = ids[src] #else: print ids[src]
            dst = '\t'.join([parts[x] for x in py_val_args])
            if dst not in vals:
                print parts
                foo = dict(vals)
                for k,v in vals.iteritems():
                    if v>69:del foo[k]
                    if v<60:del foo[k]
                print foo
                assert "Problematic column id: \"%s\" on line %d of %s" % (dst,ln,infile)       
            dstkey=-1
            try:
                dstkey = vals[dst] #else: print vals[dst]
            except KeyError:
                print "Problematic column id: \"%s\" on line %d of %s" % (dst,ln,infile)
                exit(1)
            wt = 1.0
            if weighted:
                wt = float(parts[-1])
            vectors.itemset(
                (srckey,dstkey),
                wt
                )
        print "%d values set" % ln
    
    print "saving to %s..." % outfile
    with open(outfile,'w') as f:
        #print "saving ids"
        cPickle.dump(ids, f, protocol=2)
        #print "saving vectors"
        cPickle.dump(vectors, f, protocol=2)
        #print "closing file"
    print "done."

            
if __name__=='__main__':
    if len(sys.argv)<3:
        print "Usage:\n\t%s dataset.cfacts dataset.theano.pkl [row,id,args column,id,args] [weighted]" % sys.argv[0]
        print " ids are 1-indexed (as for `cut`)"
        exit(1)
    args = sys.argv[1:]
    if "+" in args:
        i = args.index("+")
        args = args[:i]+args[(i+1):]
    cfacts2theano(*args)
