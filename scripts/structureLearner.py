#!/usr/bin/python

import sys
import re
from subprocess import call

##############################################################################
# Structure-learner for a sublanguage of ProPPR.
# 
# Input: a set of training examples for intensionally-defined
# predicates in the file STEM-train.trainData, and a 'second-order'
# theory, defined by the rules below.  The second-order theory defines
# the predicate interp/3, calls the single extensionally-defined
# predicate rel/3, and its features must correspond to rules for the
# learned predicate interp0/3, with the correspondence determined by
# the function feature2rule.  The learned predicate interp0/3
# initially contains a single rule, INTERP0_BASE_CASE_RULE.
# 
# Output: a learned rule set named STEM.fig-rules, which also
# defines interp0/3, and incidentally reduces interp/3 to interp/0 via
# the INTERP_BASE_CASE_RULE.
#
# Also, a bunch of tmp/debug files called STEM-nn.*
#
# Algorithm, roughly: 
#   learnedRules = emptySet
#   for i = i .. MAX_ITERS
#      1) add all r in learnedRules to SECOND_ORDER_THEORY as additional
#         rules for interp0
#      2) train for a few epochs, and compute the gradient of the learned 
#         theory.
#      3) examine the features with negative gradient, create new rules
#         for them, and add them to learnedRules.  exit if there are
#         no new rules.
#
# dependencies: this needs to have your path and classpath set so you
# can run the FindGradient main with java.
##############################################################################

####################
# parameters

# where to find the rule compiler
RULE_COMPILER = '~wcohen/code/Praprolog/scripts/rulecompiler.py' 

# number of times to add new rules for interp0 to the second-order theory
MAX_ITERS = 10

#the second-order theory

INTERP0_BASE_CASE_RULE = 'interp0(P,X,Y) :- rel(P,X,Y) #fixedWeight.'
INTERP_BASE_CASE_RULE = 'interp(P,X,Y) :- interp0(P,X,Y) #fixedWeight.'
SECOND_ORDER_THEORY = [
    'interp(P,X,Y) :- interp0(R,X,Y), abduce_if(P,R) #fixedWeight.',
    'interp(P,X,Y) :- interp0(R,Y,X), abduce_ifInv(P,R) #fixedWeight.',
    'interp(P,X,Y) :- interp0(R1,X,Z), interp0(R2,Z,Y), abduce_chain(P,R1,R2) #fixedWeight.',
    'abduce_if(P,R) :- # if(P,R).',
    'abduce_ifInv(P,R) :- # ifInv(P,R).',
    'abduce_chain(P,R1,R2) :- # chain(P,R1,R2).' ]


#the mapping of second-order theory features to rules

CALL_REL_DIRECTLY_FOR_BACKGROUND_PREDICATES = False
INTENSIONAL_PREDICATE_PREFIX = 'i_' #for the family data
def feature2rule(feat):
    """Map a feature from the second-order theory to a rule defining interp0/3."""
    def interpreterCall(s):
        if CALL_REL_DIRECTLY_FOR_BACKGROUND_PREDICATES and INTENSIONAL_PREDICATE_PREFIX:
            return 'interp0' if s.startswith(INTENSIONAL_PREDICATE_PREFIX) else 'rel'
        else:
            return 'interp0'
    print 'feature2rule',feat
    featureParsed = False
    m = re.match('ifInv\((\w+),(\w+)\)',feat)
    if m:
        ic = interpreterCall(m.group(2))
        return 'interp0(%s,X,Y) :- %s(%s,Y,X).' % (m.group(1),ic,m.group(2))
    m = re.match('if\((\w+),(\w+)\)',feat)
    if m:
        ic = interpreterCall(m.group(2))
        return 'interp0(%s,X,Y) :- %s(%s,X,Y).' % (m.group(1),ic,m.group(2))
    m = re.match('chain\((\w+),(\w+),(\w+)\)',feat)
    if m: 
        ic1 = interpreterCall(m.group(2))
        ic2 = interpreterCall(m.group(3))
        return 'interp0(%s,X,Y) :- %s(%s,X,Z),%s(%s,Z,Y).' % (m.group(1),ic1,m.group(2),ic2,m.group(3))
    return None

# number of epochs to train on the i-th round

def numEpochsOnRound(i): return i+1
    

# prover/learner to use in gradient computation

PROVER = 'dpr:0.001'
SRW = 'l2p:0.001:20'
COOKER = 'mmc:20'
TRAINER = 'trove.mrr:20'

#################### main algorithm

def structureLearner(stem):
    """Structure-learning for ProPPR.
    """

    baseFindGradientCommand = [
        'java','-Xmx16g','edu.cmu.ml.praprolog.FindGradient',
        '--prover',PROVER,
        '--srw', SRW,
        '--cooker', COOKER,
        '--trainer', TRAINER,
        '--train', stem+'-train.trainData',
        ]

    # create an empty gradient file
    gradientFile0 = ithFileName(stem,0,'.gradient')
    fp0 = open(gradientFile0,'w')
    fp0.close()
    
    gradientFiles = [gradientFile0]
    gradientFeatureSet = set()

    #iteratively find new features/rules to add

    for i in range(MAX_ITERS):

        print '- starting pass ',i,'now'

        #accumulate all the gradient features into a new 2nd-order ruleset
        (ruleFile,cruleFile,newGradientFeatureSet) = rulesFromGradient(i,stem,gradientFiles)
        catfile(ruleFile,('- generated rules for pass %d' % i))

        #check convergence
        print len(newGradientFeatureSet),'gradientFeatures found on iteration',i
        if i>0 and len(newGradientFeatureSet)==len(gradientFeatureSet):
            print '- no new features produced at iteration',i,'...halting'
            break
        else:
            gradientFeatureSet = newGradientFeatureSet
        
        nextGradientFile = ithFileName(stem,i+1,'.gradient')
        numEpochs = numEpochsOnRound(i)
        print '- gradient computed after ',numEpochs,'epochs'
        tcall(baseFindGradientCommand +
              ['--params', nextGradientFile,
               '--programFiles', stem+'-train.cfacts:' + cruleFile,
               '--epochs', str(numEpochs),
               '--output', ithFileName(stem,i,'.grounded'),
               ])
        gradientFiles += [nextGradientFile]

    # end of FOR loop iteration

    #create some 'final' iterated gradient (fig) rule files
    gradient2Rules(stem+'.fig-rules',gradientFiles,addSecondOrderRules=False)
    catfile(stem+'.fig-rules','final iterated gradient with out 2nd-order rules')
    tcall(['python',RULE_COMPILER,stem+'.fig-rules',stem+'.fig-crules'])

    #gradient2Rules(stem+'.fig2-rules',gradientFiles,addSecondOrderRules=True)
    #catfile(stem+'.fig2-rules','final iterated gradient with 2nd-order rules')
    #tcall(['python',RULE_COMPILER,stem+'.fig2-rules',stem+'.fig2-crules'])
             
def rulesFromGradient(i,stem,gradientFiles):
    ruleFile = ithFileName(stem,i,'.g-rules')
    featureSet = gradient2Rules(ruleFile,gradientFiles)
    cruleFile = ithFileName(stem,0,'.crules')
    tcall(['python','../scripts/rulecompiler.py',ruleFile,cruleFile])
    print '-- created',ruleFile,'and',cruleFile,'from',gradientFiles
    return (ruleFile,cruleFile,featureSet)


def catfile(fileName,msg):
    """Print out a created file - for  debugging"""
    print msg
    print '+------------------------------'
    for line in open(fileName):
        print ' |',line,
    print '+------------------------------'

def gradient2Rules(ruleFile,gradFiles,addSecondOrderRules=True):
    """Compile a list of gradient files into a rule file"""
    fp = open(ruleFile,'w')
    featureSet = set()

    for (gfile) in gradFiles:
        print '-- compiling gradient file',gfile,'to rules in',ruleFile
        fp.write('#### from gradient file %s ####\n' % gfile)
        for line in open(gfile):
            (feat,scoreStr) = line.strip().split("\t")
            score = float(scoreStr)
            if score<0:
                #print '---compiling feature',score,feat
                if feat not in featureSet:
                    rule = feature2rule(feat)
                    if rule:
                        fp.write(rule + '\n')
                        featureSet.add(feat)
                    else:
                        print '---?? unparsed feature',feat

                        
    fp.write(INTERP0_BASE_CASE_RULE + '\n')
    if not addSecondOrderRules:
        fp.write(INTERP_BASE_CASE_RULE + '\n')
    else:
        fp.write('#### from pre-defined SECOND_ORDER_THEORY  ####\n')
        for rule in SECOND_ORDER_THEORY:
            fp.write(rule + '\n')
    return featureSet


#################### file construction and command execution

def ithFileName(stem,i,extension):
    return stem+'-'+('%02d' % i)+extension

def tcall(xs): 
    """Call command in list xs, with a trace."""
    print '--calling',xs
    call(xs)


if __name__ == "__main__":

    stem = sys.argv[1]
    structureLearner(stem)
