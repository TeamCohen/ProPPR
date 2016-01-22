#!/usr/bin/python

"""
1. locate theano features theano_p(*,x)
2. build feature vector for x
3. for each y in *:
3.1 compute p(y|x)
3.2 store weight
4. scan through ground file and assign edge weights
"""

import sys
import cPickle
import gzip
import os
import shutil
import numpy
import theano
import theano.tensor as T
import pronghorn

#for main commands
shortHelpMsg = {}    
action = {}

def doMain():
    com = getArg(0)
    if com not in action:
        print "Illegal command '%s'\n" % com
        com = "help"
    action[com]()

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

def featureToXY(f) :
    (y,x) = f[N_THEANO_PREFIX:f.index(")")].split(",")
    return (x,y)

def updateParamsFile(paramsFile,tfindex,scores):
    if os.path.isfile(paramsFile):
        print "Updating feature weights in params file %s..." % paramsFile
        backup = makebackup(paramsFile)
        shutil.copyfile(paramsFile,backup)
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
                w.write(p)
                w.write(d)
                w.write("%g" % scores[tfindex[p]])
                w.write("\n")
                ntotalmod+=1
            print "\n%d total modifications" % ntotalmod
    else:
        print "Writing feature weight to params file %s..." % paramsFile
        with open(paramsFile,'wb') as w:
            for p,i in tfindex.iteritems():
                w.write(p)
                w.write("\t")
                w.write("%g" % scores[i])
                w.write("\n")
            print "\n%d total features" % len(tfindex)

def updateGroundFile(groundFile,itf,tfindex,scores):
    print "Updating edge weights in ground file %s..." % groundFile
    backup = makebackup(groundFile)
    shutil.copyfile(groundFile,backup)
    with open(backup,'rb') as r, open(groundFile,'wb') as w:
        ntotalmod=0
        # for each example:
        for line in r:
            line=line.strip()
            if line[0]=="#": continue
            nmod=0 # track number of modified weights in each graph
            N=len(line)
            ahead = line.index(">")
            end = line[:ahead].rindex("\t")
            w.write(line[:end])
            # for each edge:
            while ahead>0:
                cursor = end
                ahead = line.find(">",ahead+1)
                end = N
                if ahead>0: end = line[:ahead].rindex("\t")
                (edge,d,flist) = line[cursor:end].partition(":")
                w.write(edge)
                w.write(d)
                nth = False
                # loop through features
                # if feature not in tf (make inverted tf), write as is
                # else write with new score
                for fspec in flist.split(","):
                    if nth: w.write(",")
                    nth = True
                    (fid,d,wt) = fspec.partition("@")
                    ifid = int(fid)
                    if ifid not in itf: 
                        w.write(fspec)
                        continue
                    w.write(fid)
                    w.write(d)
                    w.write("%g" % scores[tfindex[itf[ifid]]])
                    nmod+=1
            w.write("\n")
            if nmod>0: print "%d weights modified" % nmod
            ntotalmod += nmod
        print "\n%d total modifications" % ntotalmod


def train():
    groundFile = getArg(1)
    featureFile = groundFile+".features"
    theanoVectors = getArg(2)
    theanoModel = getArg(3)
    print "Loading..."
    (vectorIndex,vectors,targets) = (0,0,0)
    with open(theanoVectors,'r') as f:
        vectorIndex = cPickle.load(f)
        vectors = cPickle.load(f)
        targetIndex = cPickle.load(f)
        targets = cPickle.load(f)
    print "Training..."
    p = pronghorn.Pronghorn(pronghorn.LogisticRegression)
    classifier = p.train(theanoModel,vectors,T.cast(targets,'int32'))
shortHelpMsg['train'] = ": dataset.grounded[i] theanoVectors.pkl[i] theanoModel.pkl[o]"
action['train'] = train

def update():
    groundFile = getArg(1)
    gradientFile = getArg(2)
    theanoVectors = getArg(3)
    theanoModel = getArg(4)
    (tf,itf)=(0,0)
    paramsFile = 0
    if groundFile.endswith("grounded"):
        featureFile = groundFile+".features"
        paramsFile = groundFile[:groundFile.rindex(".")]+".params"
    elif groundFile.endswith("params"):
        featureFile = groundFile
        paramsFile = groundFile
    print "Loading features from %s..." % featureFile
    (tf,itf) = getTheanoFeatures(featureFile)
    print "Loading vectors from %s..." % theanoVectors
    (vectorIndex,vectors,targets) = (0,0,0)
    with open(theanoVectors,'r') as f:
        vectorIndex = cPickle.load(f)
        vectors = cPickle.load(f)
    print "Constructing theano training data..."
    N=vectors.shape[1]
    dldw = numpy.zeros(len(tf),dtype=theano.config.floatX)
    trainX = numpy.zeros( (len(tf),N*2),dtype=theano.config.floatX )
    trainY = numpy.zeros( len(tf),dtype='int32' )
    tfindex = {}
    i=0
    print "Loading gradient from %s..." % gradientFile
    with open(gradientFile,'r') as f:
        ln = 1
        for line in f:
            line=line.strip()
            if line[0] == "#": continue
            (feature,wt) = line.split("\t")
            if feature in tf:
                tfindex[feature] = i
                dldw.itemset(i,float(wt))
                (x,y) = featureToXY(feature)
                trainX[i,:N] = vectors[vectorIndex[x],:]
                trainX[i,N:] = vectors[vectorIndex[y],:]
                trainY[i] = 1 #(0,1)[dldw[i]>0] # 0 for similar; 1 for dissimilar
                i+=1
    print "Performing 1 update to theano model in %s..." % theanoModel
    p = pronghorn.Pronghorn(pronghorn.SimilarityRegression)
    classifier = p.update(theanoModel,dldw,trainX,trainY)
    scores = p.score(trainX,trainY,classifier)
    # update/create paramsFile
    updateParamsFile(paramsFile,tfindex,scores)
shortHelpMsg['update'] = ": dataset.grounded dataset.gradient theanoVectors.pkl theanoModel.pkl"
action['update'] = update

def answer():
    groundFile = getArg(1)
    featureFile = groundFile+".features"
    theanoVectors = getArg(2)
    theanoModel = getArg(3)
    print "Loading..."
    (xs,ys) = sortTheanoFeatures(getTheanoFeatures(featureFile))
    (vectorIndex,vectors,targets) = (0,0,0)
    with open(theanoVectors,'r') as f:
        vectorIndex = cPickle.load(f)
        vectors = cPickle.load(f)
    classifier = 0
    with open(theanoModel,'r') as f:
        classifier = cPickle.load(f)
    p = pronghorn.Pronghorn(pronghorn.LogisticRegression)
    scores = p.score(vectors,[0,1],classifier)
    #print "%10s\ty=%6s\ty=%6s" % ("query",targetIndex[0],targetIndex[1])
    for qi in range(len(vectorIndex)):
        print "%10s\t%g\t%g" % (vectorIndex[qi],scores[0].item(qi),scores[1].item(qi))
shortHelpMsg['answer'] = ": dataset.grounded theanoVectors.pkl theanoModel.pkl"
action['answer'] = answer

def doHelp():
   print 'ProPPR-theano: commands are:'
   for com in sorted(shortHelpMsg.keys()):
      print ('  %s ' % sys.argv[0]) + com + shortHelpMsg[com]
shortHelpMsg['help'] = ': this help message'
action['help'] = doHelp

def getArg(i,defaultVal=None):
   """Get the i-th command line argument."""
   i = i+1
   def safeDefault():
      if defaultVal: 
         return defaultVal
      else:
         os.write(2,"expected at least %d command-line arguments - use 'help' for help\n" % (i))
         sys.exit(-1)
   try:
      result = sys.argv[i]
      return result if not result.startswith("--") else safeDefault()
   except IndexError:
      return safeDefault()

if __name__=='__main__':
    doMain()
    
