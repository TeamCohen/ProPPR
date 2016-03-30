import sys
import collections
import getopt
import re
import copy

#################### parsing input files 

def answerWithIntVars(query):    
    """Convert from format p(foo,X1,X2) to p(foo,-1,-2)."""        
    return re.sub(r"(\W)[_A-Z]\w*([0-9]+)",r"\1-\2",query)

def queryWithIntVars(query):
    """Convert from format p(foo,Y,Z) to p(foo,-1,-2)."""
    result = query[:]
    # bugfix: variables must start with _A-Z, which means they must be preceded by ^H^Ha nonword character
    # bugfix: variables must start with _A-Z, which means they must be preceded by a comma , or open paren (
    matches = list(re.finditer(r"([,(])[_A-Z]\w*", query))
    k = -len(matches)
    for m in reversed(matches):
        result = result[:m.start()] + m.groups()[0] + str(k) + result[m.end():]
        k += 1
    return result

class Labels(object):

    """Encodes labels associated with a training or test data file,
    ie the input files for ProPPR trainers, where lines are

        query <TAB> +solution <TAB> ... <TAB> -solution

    where query is a ProPPR query with variables and the solutions are
    the same query with constants substituted for the variables.  In a
    labels object, the same information is parsed into python data
    structures, as follows

    - labels.queries is a set of all the queries, with integer
      constants replacing the variables.
    - labels.pos[q] is a set of positive solutions for query q
    - labels.neg[q] is the negative solutions
"""

    def __init__(self,dataFile):
        """Load labels from the datafile."""
        self.dataFile = dataFile
        self.pos = collections.defaultdict(set)
        self.neg = collections.defaultdict(set)
        self.queries = set()
        linenum=0
        for line in open(self.dataFile):
            linenum += 1
            parts = line.strip().split("\t")
            query = parts[0]
            intVarQuery = queryWithIntVars(query)
            if intVarQuery in self.queries:
                for p in parts[1:]:
                    if (p.startswith('+') and p[1:] not in self.pos[intVarQuery]) or (p.startswith('-') and p[1:] not in self.neg[intVarQuery]):
                        assert "Duplicate query %s at line %d with different labels. Not sure what you want me to do here, since solution files may not be in query file order." % (query,linenum)
            else:
                self.queries.add(intVarQuery)
                for p in parts[1:]:
                    if p.startswith('+'):
                        self.pos[intVarQuery].add(p[1:])
                    elif p.startswith('-'):
                        self.neg[intVarQuery].add(p[1:])
                    else:
                        assert 'somethings wrong at line ' + line
        
    def __str__(self):
        return 'Labels(%s,%s)' % (str(self.pos),str(self.neg))

    def __repr__(self):
        return 'Labels(%s)' % self.dataFile

    def show(self):
        for q in self.queries:
            print '%s\t' % q,
            self.showQuery(q)
    
    def showQuery(self,q):
        print "\t",len(self.pos[q]),"pos",len(self.neg[q]),"neg label",
        if self.pos[q]:
            print '\t'.join(map(lambda s: ('+%s'%s), list(self.pos[q]))),
        if self.neg[q]:
            print '\t'.join(map(lambda s: ('-%s'%s), list(self.neg[q]))),
        print


def adversariallyOrdered(answerList):
    def adverseKey(a): return (1.0-a.score,a.isPos,a.rank)
    resorted = sorted([(adverseKey(a),a) for a in answerList])
    for i in range(len(resorted)):
        (keyOfA,a) = resorted[i]
        resorted[i] = copy.copy(a)
        resorted[i].rank = i+1
    #print '%3d |%s\t|\t%s' % (i,answerList[i].asFields(" "),resorted[i].asFields(" "))
    return resorted


class Answer(object):
    """Encodes a single answer proposed by ProPPR for a query."""
    def __init__(self,rank,score,solution):
        self.rank = rank
        self.score = score
        self.solution = solution
        self.isPos = None
        self.isNeg = None

    def asFields(self,sep="\t"):
        return sep.join(map(str,[self.rank,self.score,self.solution,["","+"][self.isPos],["","-"][self.isNeg]]))        

    def __repr__(self):
        return 'Answer(rank=%d,score=%f,isPos=%s,isNeg=%s,solution=%s)' \
            % (self.rank,self.score,str(self.isPos),str(self.isNeg),repr(self.solution))

class Answers(object):
    """ Encodes answers produced by QueryAnswerer, as follows.
    For any query q in the answer file:

    - answers.queryTime[q] is proof time in msec 
    - answers.answers[q] is a list of Answer objects, which contain
      the attributes rank, score, solution, isPos and isNeg.
    - answers.solutions[q] is a set of all the solutions returned for
      q

    Any answers that are not either isPos or isNeg - ie, unknown truth
    values - are discarded by default. To automatically label unknown 
    solutions negative, run with --defaultNeg
"""
    
    def __init__(self,answerFile,labels=None,defaultNeg=False):
        """Parse an answer file - if labels are not given, then
        the isPos/isNeg flags will be set to None."""
        #print "defaultNeg:",defaultNeg
        self.answerFile = answerFile
        self.answers = collections.defaultdict(list)
        self.solutions = collections.defaultdict(set)
        self.queryTime = {}
        done = {}
        intVarQuery = None
        totQueries = 0
        totAnswers = 0
        totLabeledAnswers = 0
        for line in open(self.answerFile):        
            if line.startswith('#'):
            	done[intVarQuery] = True
                (dummy,intVarQuery,timeStr) = line.strip().split("\t")
                intVarQuery = answerWithIntVars(intVarQuery[:(intVarQuery.rindex("."))])
                if intVarQuery in done: continue #skip duplicate queries
                totQueries += 1
                self.queryTime[intVarQuery] = int(timeStr.split(" ")[0])
                #print line
                #print intVarQuery
                #print "+",labels.pos[intVarQuery]
                #print "-",labels.neg[intVarQuery]
            else:
            	if intVarQuery in done: continue #skip duplicate queries
                totAnswers += 1
                (rankStr,scoreStr,solution) = line.strip().split("\t")
                score = float(scoreStr)
                rank = int(rankStr)
                solution = solution[:-1] #trim trailing '.'
                #theta = self.asDict(thetaStr)
                #solution = self.substitute(intVarQuery,theta)
                a = Answer(rank,score,solution)
                if labels:
                    a.isPos = solution in labels.pos[intVarQuery]
                    a.isNeg = (defaultNeg and not a.isPos) or solution in labels.neg[intVarQuery]
                    #print 'solution',solution,'intVarQuery',intVarQuery,'pos',labels.pos[intVarQuery],'neg',labels.pos[intVarQuery]
                if a.isPos or a.isNeg:
                    totLabeledAnswers += 1
                    self.answers[intVarQuery].append(a)
                    self.solutions[intVarQuery].add(solution)
                    #print line.strip(),a.isPos,a.isNeg
                #else:
                #    print line.strip()
        for q in self.answers:
            self.answers[q] = adversariallyOrdered(self.answers[q])
        print 'queries',totQueries,'answers',totAnswers,'labeled answers',totLabeledAnswers


    def asDict(self,thetaStr):
        result = {}
        for m in re.finditer(r"\-(\d+)=c\[([^\]]+)\]", thetaStr):
            result[m.group(1)] = m.group(2)
        return result

    def substitute(self,intVarQuery,theta):
        result = intVarQuery[:]
        matches = list(re.finditer(r"-([\d+])", intVarQuery))
        for m in reversed(matches):
            result = result[:m.start()] + theta[m.group(1)] + result[m.end():]            
        return result

    def show(self,summary=False):
        for query in self.answers.keys():
            print '#',query,'proved in',self.queryTime[query],'msec'
            self.showQuery(query,summary)
    
    def showQuery(self,query,summary=False):
        answerList = self.answers[query]
        for a in answerList:
            if (not summary) or (a.isPos or a.isNeg):
                print a.asFields()


#################### abstract metric class

class Metric(object):
    """Abstract class to compute an arbitrary metric."""

    def explanation(self):
        """Long name, for usage statements."""
        assert False,'abstract method called'

    def compute(self,answerList,solutionSet,posSolutionSet):
        """Compute the metric."""
        assert False,'abstract method called'

    def macroAverage(self,answers,labels):
        """Compute over the full list of answers."""
        fullAnswerList = reduce(lambda l1,l2:l1+l2, [answers.answers[q] for q in answers.answers.keys()],[])
        fullSolutionSet = reduce(lambda s1,s2:s1.union(s2), [answers.solutions[q] for q in answers.answers.keys()],set())
        fullPosSolutionSet = reduce(lambda s1,s2:s1.union(s2), [labels.pos[q] for q in labels.queries])
        return self.computeFromList(fullAnswerList,fullSolutionSet,fullPosSolutionSet)

    def microAverage(self,answers,labels):
        """Compute over each query individually and average results."""
        tot = n = 0.0
        for q in labels.queries:#answers.answers.keys():
            n += 1.0
            tot += self.computeFromList(answers.answers[q],answers.solutions[q],labels.pos[q])
        if n==0: return n
        return tot/n

    def detailedReportAsDict(self,answers,labels):
        result = {}
        for q in labels.queries:#answers.answers.keys():
            result[q] = self.computeFromList(answers.answers[q],answers.solutions[q],labels.pos[q])
        return result

#################### specific metrics

class MeanRecipRank(Metric):
    
    def explanation(self):
        return '(Mean Reciprocal Rank): averages 1/rank for all positive answers'

    def computeFromList(self,answerList,solutionSet,posSet):
        tot = n = 0.0
        for a in answerList:
            if a.isPos:
                tot += 1.0/a.rank
                n += 1
        for a in posSet:
            if not a in solutionSet:
                n += 1
        if n: return tot/n
        else: return 1.0
        
class Recall(Metric):
    
    def explanation(self):
        return '(Recall): fraction of positive examples that are proposed as solutions anywhere in the ranking'

    def computeFromList(self,answerList,solutionSet,posSet):
        r = 0.0
        n = len(posSet)
        if n == 0: return 1.0
        for a in posSet:
            if a in solutionSet:
                r += 1.0
        return r/n

class HitsAt10(Metric):
    def explanation(self):
        return '(Hits@10): The proportion of correct head- or tail-assignments with a rank no greater than 10 among all possible head- or tail-assignments. via TransE'
    def computeFromList(self,answerList,solutionSet,posSet):
        nP = len(posSet)
        if nP==0: return 1.0
        numPosRetrieved = 0.0
        numOther = 0.0
        # hits@10 ignores rank competition by other correct answers
        # so we duplicate that effect here by counting until we see 10 incorrect
        for a in adversariallyOrdered(answerList):
            if a.isPos: numPosRetrieved += 1.0
            else: numOther += 1.0
            if numOther >= 10: break
        return numPosRetrieved / nP
        
class PrecisionAt10(Metric):

    def explanation(self):
        return '(P10): Precision at rank 10'
        
    def computeFromList(self,answerList,solutionSet,posSet):
        n = len(posSet)
        if n==0: return 1.0
        numPosRetrieved = 0.0
        foo = adversariallyOrdered(answerList)
        #print "Query:"
        for a in foo[0:10]:
            if a.isPos:
                numPosRetrieved += 1.0
                #print a,'+'
            else:
                #print a,'-'
                pass
        #print 'returning',numPosRetrieved,'/',10.0
        return numPosRetrieved/10.0

class PrecisionAt1(Metric):

    def explanation(self):
        return '(P1): Precision at rank 1'
        
    def computeFromList(self,answerList,solutionSet,posSet):
        n = len(posSet)
        if n==0: return 1.0
        foo = adversariallyOrdered(answerList)
        if len(foo)==0: return 0.0
        a = foo[0]
        if a.isPos:
            return 1.0
        else:
            return 0.0

class MeanAvgPrecision(Metric):
    
    def explanation(self):
        return '(MAP): The average precision after each relevant solution is retrieved'
    
    def computeFromList(self,answerList,solutionSet,posSet):
        n = len(posSet)
        if n is 0: return 1.0
        numPosRetrieved = 0.0
        numRetrieved = 0.0
        ap = 0.0
        foo = adversariallyOrdered(answerList)
        #print "Query:"
        for a in foo:
            numRetrieved += 1.0
            if a.isPos:
                numPosRetrieved += 1.0
                ap += (numPosRetrieved / numRetrieved)
                #print ap,a.asFields()
        #print ap,"/",n,"=",ap/n
        return ap/n

class AreaUnderROC(Metric):
    def explanation(self):
        return '(AUC): The probability of a positive example scoring higher than a negative example; or the area under the ROC curve'
    def computeFromList(self,answerList,solutionSet,posSet):
        # Modified 27 May by katie to handle the following instances:
        # - where no negative labels are available
        # - where some positive labels were not retrieved
        npos = len(posSet)
        N = len(answerList)
        if N == 0: return 0.0
        if npos == 0: return 0.0
        
        # for each positive result compute
        # how many negative results rank lower
        nneg = 0
        rankSum = 0.0
        for a in answerList:
            if a.isPos:
                rankSum += N-nneg
            else:
                nneg += 1
        # convert to a probability (out of N)
        # then average over the npos positive labels
        return rankSum / (N*npos)
        
class Accuracy(Metric):
    
    def microAverage(self,answers,labels):
        # not defined
        return -1

    def computeFromList(self,answerList,solutionSet,posSet):
        # answers are rank, score, solution, isPos, isNeg
        instanceScores = collections.defaultdict(list)
        for a in answerList:
            instanceId,predictedLabel = self.parseSolution(a.solution)
            #save a bunch of stuff for testing mostly
            instanceScores[instanceId].append( (a.score,predictedLabel,a.isPos) )
        numCorrect = 0
        for instanceId in instanceScores:
            instanceScores[instanceId].sort(reverse=True)
            #print instanceId,instanceScores[instanceId]
            (score,label,isPos) = instanceScores[instanceId][0]
            if isPos: numCorrect += 1
        totExamples = len(instanceScores)
        for solution in posSet:
            instanceId,predictedLabel = self.parseSolution(solution)
            if not instanceId in instanceScores:
                totExamples += 1
        print '. accuracy notes:',numCorrect,'/',totExamples,'examples correct: typical instances',instanceScores.keys()[0:3]
        return numCorrect/float(totExamples)

    def dissectSolution(self,solution):
        functor,delim,args = solution[:-1].partition("(")
        arg1,delim,arg2 = args.partition(",")
        return functor,arg1,arg2

class AccuracyL1(Accuracy):            

    def explanation(self):
        return '(Accuracy L1): accuracy where goals are of the form predict(Y,X), ie label is first argument.'

    def parseSolution(self,solution):
        (f1,y,x) = self.dissectSolution(solution)
        return x,y

class AccuracyL2(Accuracy):            

    def explanation(self):
        return '(Accuracy L2): accuracy where goals are of the form predict(X,Y), ie label is second argument.'

    def parseSolution(self,solution):
        (f1,x,y) = self.dissectSolution(solution)
        return x,y

class MeanRecipRank(Metric):
    
    def explanation(self):
        return '(Mean Reciprocal Rank): averages 1/rank for all positive answers'

    def computeFromList(self,answerList,solutionSet,posSet):
        tot = n = 0
        for a in answerList:
            if a.isPos:
                tot += 1.0/a.rank
                n += 1
        for a in posSet:
            if not a in solutionSet:
                n += 1
        if n: return tot/n
        else: return 1.0
        

####################  main

if __name__ == "__main__":

    metrics = {'mrr':MeanRecipRank(), 'recall':Recall(), 'p10':PrecisionAt10(), 'p1':PrecisionAt1(), 'h10':HitsAt10(), 
               'map':MeanAvgPrecision(), 'acc1':AccuracyL1(), 'acc2':AccuracyL2(), 'auc':AreaUnderROC()}

    argspec = ["data=", "answers=", "metric=", "defaultNeg", "help", "debug", "echo", "details"]
    optlist,remainingArgs = getopt.getopt(sys.argv[1:], "", argspec)
    option = dict(optlist)
    if ('--data' not in option) or ('--answers' not in option) or ('--help' in option):
        print 'usage:',sys.argv[0],'--data TRAIN-OR-TEST-FILE --answers ANSWER-FILE --metric M'
        for key in sorted(metrics.keys()):
            print '  --metric',key,metrics[key].explanation()
        print
        print '  --echo will print a representation of the raw information from which metrics are computed'
        sys.exit(-1)
        
    labels = Labels(option['--data'])
    answers = Answers(option['--answers'],labels,'--defaultNeg' in option)

    if '--debug' in option:
        for query in labels.queries:
            print "#",query
            labels.showQuery(query)
            answers.showQuery(query)
        #print 'labels:'
        #labels.show()
        #print 'answers:'
        #answers.show(summary=False)

    if ('--echo' in option):
        print 'labeled answers: rank score solution isPos isNeg'
        answers.show(summary=False)

    for (key,val) in optlist:
        if key=='--metric':
            if val in metrics:
                print '=' * 78
                print 'metric %s %s' % (val,metrics[val].explanation())
                micro = metrics[val].microAverage(answers,labels)
                macro = metrics[val].macroAverage(answers,labels)
                print '. micro:',micro
                print '. macro:',macro
                if '--details' in option:
                    print '. details:'
                    d = metrics[val].detailedReportAsDict(answers,labels)
                    for q in d:
                        print '. . ',q,'\t',d[q]
            else:
                print 'unknown metric',val,'use --help for help'
