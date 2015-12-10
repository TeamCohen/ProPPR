import sys
import os
import re
import getopt
import random
import logging
import subprocess
import collections


def lift(src,dst,opts):
    """Convert arity-two facts P(X,Y) to second-order representation rel(P,X,Y)."""
    fp = open(dst,'w')
    for line in open(src):
        line = line.strip()
        if not line or line.startswith("#"):
            fp.write(line + '\n')
        else:
            if len(line.split("\t"))!=3:
                logging.warn('bad line from %s ignored: %s' % (src,line.strip()))
            else:
                fp.write('rel\t' + line + '\n')
    logging.info('second-order version of facts from '+ src + ' stored in ' + dst)
    
def lower(src,dst,opts):
    """Convert second-order representation rel(P,X,Y) back to arity-two facts P(X,Y)."""
    fp = open(dst,'w')
    for line in open(src):
        line = line.strip()
        if not line or line.startswith("#"):
            fp.write(line + '\n')
        else:
            parts = line.split("\t")
            fp.write("\t".join(parts[1:]) + "\n")
    logging.info('first-order version of facts from '+ src + ' stored in ' + dst)

def relationsToExamples(src,dst,opts):
    #TODO sampling options
    rnd = random.Random()
    trueYs = collections.defaultdict(set)
    pairedWith = collections.defaultdict(set)
    triples = set()
    entities = set()
    rels = set()
    for line in open(src):
        (relkw,r,x,y) = line.strip().split("\t")
        trueYs[(r,x)].add(y)
        rels.add(r)
        entities.add(x)
        entities.add(y)
        triples.add((r,x,y))
        pairedWith[x].add(y)
    result = []
    for r in rels:
        for x in entities:
            query = 'interp(i_%s,%s,Y)' % (r,x)
            posParts = map(lambda y: '+interp(i_%s,%s,%s)' % (r,x,y), trueYs[(r,x)])
            #TODO randomly sample negatives?
            negParts = map(lambda y: '-interp(i_%s,%s,%s)' % (r,x,y), [y for y in pairedWith[x] if y not in trueYs[(r,x)]])
            result.append((query,posParts,negParts))
    rnd.shuffle(result)
    fp = open(dst,'w')
    for (query,posParts,negParts) in result:
        fp.write(query + '\t' + '\t'.join(posParts) + '\t' + '\t'.join(negParts) + '\n')
    logging.info('example version of facts from '+ src + ' stored in ' + dst)            

def gradientToRules(src,dst,opts):
    rules = []
    rhs = opts.get('--rhs','learnedPred')
    lhs = opts.get('--lhs','learnedPred')

    for line in open(src):
        if not line.startswith("#"):
            (feature,weightStr) = line.strip().split("\t")
            weight = float(weightStr)
            if weight<0:
                parts = filter(lambda x:x, re.split('\W+', feature))
                if len(parts)==3:
                    (iftype,p,q) = parts
                    if iftype=='if' and p!=q:
                        rules.append( "%s(%s,X,Y) :- %s(%s,X,Y)." % (lhs,p,rhs,q))
                    elif iftype=='ifInv':
                        rules.append( "%s(%s,X,Y) :- %s(%s,Y,X)." % (lhs,p,rhs,q))
                elif len(parts)==4:
                    (chaintype,p,q,r) = parts                
                    if chaintype=='chain':
                        rules.append( "%s(%s,X,Y) :- %s(%s,X,Z), %s(%s,Z,Y)." % (lhs,p,rhs,q,rhs,r))
    fp = open(dst,'w')
    fp.write("\n".join(rules) + "\n")

def stucturedGradient(src,dst,opts):
    #work out inputs/outputs
    exampleFile = src
    learnedRuleFile = dst
    backgroundFile = optdict['--src2']
    exampleStem = optdict['--stem']

    #get the interpreter and compile it, then ground the examples
    interpFile = _getResourceFile(opts, "sg-interp-train.ppr")
    invokeProppr(opts,'compile',interpFile)
    programFileList =  interpFile[:-4]+'.wam:'+backgroundFile
    invokeProppr(opts,'ground',exampleFile,exampleFile+".grounded",'--programFiles',programFileList)

    #store gradient in a temp file
    gradientFile = exampleStem+'.gradient'
    invokeProppr(opts,'gradient',exampleFile+".grounded",gradientFile,'--epochs','0')

    #convert the gradient features to rules interp(R,X,Y) :- BODY where BODY contains calls to rel(R,X,Y).l
    gradientToRules(gradientFile, learnedRuleFile, {'--lhs':'interp','--rhs':'rel'})


def iterativeStucturedGradient(src,dst,opts):
    #work out inputs/outputs
    exampleFile = src
    learnedRuleFile = dst
    backgroundFile = optdict['--src2']
    exampleStem = optdict['--stem']
    numIters = int(optdict['--numIters'])

    #copy the initial interpreter to this directory
    interpFile = _getResourceFile(opts, "sg-interp-train.ppr")
    learnedRuleFiles = []

    #iteratively learn
    for i in range(numIters):
        logging.info('training pass %i' % i)

        #create the i-th interpreter, which contains the basic interpreter rules, plus all the learned rules
        interpFile = 'sg-interp_n%02d.ppr' % i
        numAddedThisRound = _appendUniqLines(['sg-interp-train.ppr']+learnedRuleFiles,interpFile)
        if numAddedThisRound==0:
            logging.info('no new rules learned in previous iteration - stopping')
            break
        _catfile(interpFile,'Interpreter used at round %d' % i)

        #compile the interpreter
        invokeProppr(opts,'compile',interpFile)

        #ground the examples using the interpreter + learned rules
        programFileList =  interpFile[:-4]+'.wam:'+backgroundFile
        invokeProppr(opts,'ground',exampleFile,exampleFile+".grounded",'--programFiles',programFileList)

        #compute the gradient
        gradientFile = exampleStem+'_n%02d.gradient' % i
        invokeProppr(opts,'gradient',exampleFile+".grounded",gradientFile,'--epochs','0')

        #convert the gradient features to rules interp(R,X,Y) :- BODY where BODY contains calls to rel(R,X,Y).
        nextLearnedRuleFile = '%s-learned_n%02d.ppr' % (exampleStem,i)
        gradientToRules(gradientFile,nextLearnedRuleFile,opts)
        logging.info('Created rule file ' + nextLearnedRuleFile)
        _catfile(nextLearnedRuleFile,'Rules learned in round %d' % i)

        #add this to the list of learned rules
        learnedRuleFiles.append(nextLearnedRuleFile)

    #concatenate all the learned rules, replacing the interpreter with a new one
    testInterpFile = _getResourceFile(opts, "sg-interp-test.ppr")
    _appendUniqLines([testInterpFile] + learnedRuleFiles,learnedRuleFile)

def _getResourceFile(opts,filename):
    if '--n' not in opts: #not dry run
        src = os.path.join( '%s/scripts/proppr-helpers/%s' % (os.environ['PROPPR'], filename))
        dst = filename
        fp = open(dst,'w')
        for line in open(src):
            fp.write(line)
        logging.info('copied %s to current directory' % src)
    return filename

def _appendUniqLines(inputs,output):
   previousLines = set()
   fp = open(output,'w')
   for f in inputs:
      numAddedFromLastFile = 0
      for line in open(f): 
         if line not in previousLines:
            previousLines.add(line)
            numAddedFromLastFile += 1
            fp.write(line)
   fp.close()
   return numAddedFromLastFile

def _catfile(fileName,msg):
    """Print out a created file - for  debugging"""
    print msg
    print '+------------------------------'
    for line in open(fileName):
        print ' |',line,
    print '+------------------------------'

def invokeProppr(opts,*args):
    procArgs = ['%s/scripts/proppr' % os.environ['PROPPR']]
    #deal with proppr's global options
    if '--C' in opts:
        procArgs.extend(['--C', optdict['--C']])
    if '--n' in optdict:
        procArgs.extend(['--n'])
    procArgs.extend(args)
    if '--n' not in opts: #not dry run
        logging.info('calling: ' + ' '.join(procArgs))
        stat = subprocess.call(procArgs)
        if stat:
            logging.info(('call failed (status %d): ' % stat) + ' '.join(procArgs))
            sys.exit(stat) #propagate failure

if __name__=="__main__":
    logging.basicConfig(level=logging.INFO)
    argspec = ["com=","src=", "dst=", 
               "C=", "n", #global proppr opts
               "lhs=", "rhs=", #for gradientToRules
               "src2=", "numIters=", "stem=", #for iterativeStucturedGradient, structuredGradient
    ]
    try:
        optlist,args = getopt.getopt(sys.argv[1:], 'x', argspec)
    except getopt.GetoptError as err:
        print str(err)
        usage()
        system.exit(-1)
    optdict = dict(optlist)

    subcommand = optdict['--com']
    src = optdict['--src']
    dst = optdict['--dst']
    if subcommand=='lift':
        lift(src,dst,optdict)
    elif subcommand=='lower':
        lower(src,dst,optdict)
    elif subcommand=='rel2ex':
        relationsToExamples(src,dst,optdict)
    elif subcommand=='grad2ppr':
        gradientToRules(src,dst,optdict)
    elif subcommand=='isg-train':
        iterativeStucturedGradient(src,dst,optdict)
    elif subcommand=='sg-train':
        stucturedGradient(src,dst,optdict)
    else:
        assert False,'does not compute '+subcommand
