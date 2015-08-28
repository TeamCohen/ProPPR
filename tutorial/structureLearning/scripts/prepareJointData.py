import sys
import re
import random
import collections

DUPLICATE_TRAIN_AND_BACKGROUND = True

#Generate a dataset of examples.  Possible instances of a relation
#P(X,Y) are all pairs X,Y that appear as arguments of some relation.
#All possible first arguments x are used to produce a query p(x,Y),
#and all possible instances are listed as pos/neg answers to the query
#P(x,Y).

#command-line argument is the set of relations to use as background data,
#colon-separated, the the set of relations to use as training

rels = set('father mother husband wife son daughter brother sister uncle aunt nephew niece'.split(' '))
trainFam = set('andrew arthur charles charlotte christine christopher colin james jennifer margaret penelope victoria'.split(' '))
testFam = set('alfonso angela emilio francesca gina lucia marco maria pierro roberto sophia tomaso'.split(' '))

def asExamples(facts,instances):
    print "asExamples facts:"
    print "\n ".join(sorted([str(f) for f in facts]))
    print "asExamples instances:"
    print "\n ".join(sorted([str(i) for i in instances]))
    rnd = random.Random()
    rnd.seed(None)
    trueYs = collections.defaultdict(set)
    people = set()
    for (r,x,y) in facts:
        trueYs[(r,x)].add(y)
        people.add(x)
    result = []
    print "asExamples rels:"
    for r in rels:
        for x in people:
            query = 'interp(i_%s,%s,Y)' % (r,x)
            pos = []
            neg = []
            print r,x,trueYs[(r,x)]
            for y in trueYs[(r,x)]:
                pos +=  [('interp(i_%s,%s,%s)' % (r,x,y))]
            for y in people:
                if ((x,y) in instances) and (y not in trueYs[(r,x)]):
                    neg += [('interp(i_%s,%s,%s)' % (r,x,y))]
            result += [(query,pos,neg)]
    rnd.shuffle(result)
    return result

def loadKinship(fileName):
    facts = set()
    instances = set()
    for line in open(fileName):
        (dummy,rel,x,y) = line.strip().split('\t')
        facts.add((rel,x,y))
        instances.add((x,y))
    return facts,instances

def splitFacts(facts,fam,pTrain,pSkip):
    examples = set()
    background = set()
    rnd = random.Random()
    rnd.seed(None)
    for (r,x,y) in facts:
        if x in fam:
            selectedForTrain = (rnd.random()<pTrain)
            selectedForBackground = (rnd.random()>pSkip)
            if selectedForTrain:
                examples.add((r,x,y))
            if selectedForBackground and (not selectedForTrain or DUPLICATE_TRAIN_AND_BACKGROUND):
                background.add((r,x,y))
    return examples,background

if __name__ == "__main__":
    if len(sys.argv)!=5:
        print('usage: trainStem testStem pTrain pSkip')
        sys.exit(-1)

    trainStem = sys.argv[1]
    testStem = sys.argv[2]
    pTrain = float(sys.argv[3])
    pSkip = float(sys.argv[4])

    facts,instances = loadKinship('kinship.cfacts')
    trainExampleFacts,trainBackgroundFacts = splitFacts(facts,trainFam,pTrain,pSkip)
    testExampleFacts,testBackgroundFacts = splitFacts(facts,testFam,pTrain,pSkip)

    print trainStem+".cfacts holds background facts for trainFam"
    fp = open(trainStem+'.cfacts','w')
    for (p,x,y) in trainBackgroundFacts:
        #print 'bg',p,x,y
        fp.write(('rel\t%s\t%s\t%s\n' % (p,x,y)))
    fp.close()
    print testStem+".cfacts holds background facts for testFam"
    fp = open(testStem+'.cfacts','w')
    for (p,x,y) in testBackgroundFacts:
        fp.write('rel\t%s\t%s\t%s\n' % (p,x,y))
    fp.close()

    print trainStem+".trainData holds training facts for trainFam"
    fp = open(trainStem+'.trainData','w')
    for (q,pos,neg) in asExamples(trainExampleFacts,instances):
        fp.write(q)
        for px in pos: fp.write('\t+' + px)
        for nx in neg: fp.write('\t-' + nx)
        fp.write('\n')
    fp.close()
    print testStem+".testData holds training facts for testFam"
    fp = open(testStem+'.testData','w')
    for (q,pos,neg) in asExamples(testExampleFacts,instances):
        fp.write(q)
        for px in pos: fp.write('\t+' + px)
        for nx in neg: fp.write('\t-' + nx)
        fp.write('\n')
    fp.close()
