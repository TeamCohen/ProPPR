import sys
import collections
import getopt
import re
import copy

#################### parsing input files 

def answerWithIntVars(query):    
    """Convert from format p(foo,X1,X2) to p(foo,-1,-2)."""        
    return re.sub(r"[_A-Z]\w*([0-9]+)",r"-\1",query)

def queryWithIntVars(query):
    """Convert from format p(foo,Y,Z) to p(foo,-1,-2)."""
    result = query[:]
    matches = list(re.finditer(r"[_A-Z]\w*", query))
    k = -len(matches)
    for m in reversed(matches):
        result = result[:m.start()] + str(k) + result[m.end():]
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
        for line in open(self.dataFile):
            parts = line.strip().split("\t")
            query = parts[0]
            intVarQuery = queryWithIntVars(query)
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
            if self.pos[q]:
                print '\t'.join(map(lambda s: ('+%s'%s), list(self.pos[q]))),
            if self.neg[q]:
                print '\t'.join(map(lambda s: ('-%s'%s), list(self.neg[q]))),
            print "\t",len(self.pos[q]),"pos",len(self.neg[q]),"neg label"

class Answer(object):
    """Encodes a single answer proposed by ProPPR for a query."""
    def __init__(self,rank,score,solution):
        self.rank = rank
        self.score = score
        self.solution = solution
        self.isPos = None
        self.isNeg = None

    def asFields(self,sep="\t"):
        return sep.join(map(str,[self.rank,self.score,self.solution,self.isPos,self.isNeg]))        

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

"""
    
    def __init__(self,answerFile,labels=None):
        """Parse an answer file - if labels are not given, then
        the isPos/isNeg flags will be set to None."""
        self.answerFile = answerFile
        self.answers = collections.defaultdict(list)
        self.solutions = collections.defaultdict(set)
        self.queryTime = {}
        intVarQuery = None
        for line in open(self.answerFile):        
            if line.startswith('#'):
                (dummy,intVarQuery,timeStr) = line.strip().split("\t")
                intVarQuery = answerWithIntVars(intVarQuery[:(intVarQuery.index("#")-2)])
                self.queryTime[intVarQuery] = int(timeStr.split(" ")[0])
            else:
                (rankStr,scoreStr,solution) = line.strip().split("\t")
                score = float(scoreStr)
                rank = int(rankStr)
                solution = solution[:-1] #trim trailing '.'
                #theta = self.asDict(thetaStr)
                #solution = self.substitute(intVarQuery,theta)
                a = Answer(rank,score,solution)
                if labels:
                    a.isPos = solution in labels.pos[intVarQuery]
                    a.isNeg = solution in labels.neg[intVarQuery]
                    #print 'solution',solution,'intVarQuery',intVarQuery,'pos',labels.pos[intVarQuery],'neg',labels.pos[intVarQuery]
                self.answers[intVarQuery].append(a)
                self.solutions[intVarQuery].add(solution)
        for q in self.answers:
            self.answers[q] = self.adversariallyOrdered(self.answers[q])

    def adversariallyOrdered(self,answerList):
        def adverseKey(a): return (1.0-a.score,a.isPos,a.rank)
        resorted = sorted([(adverseKey(a),a) for a in answerList])
        for i in range(len(resorted)):
            (keyOfA,a) = resorted[i]
            resorted[i] = copy.copy(a)
            resorted[i].rank = i+1
            #print '%3d |%s\t|\t%s' % (i,answerList[i].asFields(" "),resorted[i].asFields(" "))
        return resorted

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
        for query,answerList in self.answers.items():
            print '#',query,'proved in',self.queryTime[query],'msec'
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
        fullAnswerList = reduce(lambda l1,l2:l1+l2, [answers.answers[q] for q in answers.answers.keys()])
        fullSolutionSet = reduce(lambda s1,s2:s1.union(s2), [answers.solutions[q] for q in answers.answers.keys()])
        fullPosSolutionSet = reduce(lambda s1,s2:s1.union(s2), [labels.pos[q] for q in labels.queries])
        return self.computeFromList(fullAnswerList,fullSolutionSet,fullPosSolutionSet)

    def microAverage(self,answers,labels):
        """Compute over each query individually and average results."""
        tot = n = 0
        for q in answers.answers.keys():
            n += 1
            tot += self.computeFromList(answers.answers[q],answers.solutions[q],labels.pos[q])
        return tot/n

    def detailedReportAsDict(self,answers,labels):
        result = {}
        for q in answers.answers.keys():
            result[q] = self.computeFromList(answers.answers[q],answers.solutions[q],labels.pos[q])
        return result

#################### specific metrics

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
        
class Recall(Metric):
    
    def explanation(self):
        return '(Recall): fraction of positive examples that are proposed as solutions anywhere in the ranking'

    def computeFromList(self,answerList,solutionSet,posSet):
        r = 0.0
        for a in posSet:
            if a in solutionSet:
                r += 1.0
        n = len(posSet)
        if n: return r/n
        else: return 1.0
        
class MeanAvgPrecision(Metric):
    
    def explanation(self):
        return '(MAP): The average precision after each relevant solution is retrieved'
    
    def computeFromList(self,answerList,solutionSet,posSet):
        n = len(posSet)
        if n is 0: return 1.0
        numPosRetrieved = 0.0
        numRetrieved = 0.0
        ap = 0.0
        for a in answerList:
            numRetrieved += 1.0
            if a.isPos:
                numPosRetrieved += 1.0
                ap += (numPosRetrieved / numRetrieved)
        return ap/n

class AreaUnderROC(Metric):
    def explanation(self):
        return '(AUC): The probability of a positive example scoring higher than a negative example; or the area under the ROC curve'
    def computeFromList(self,answerList,solutionSet,posSet):
        npos = len(posSet)
        nneg = len(answerList) - npos
        npairs = npos * nneg
        optimumRankSum = (npos/2.0) * (npos+1.0) #=sum from 1 to npos
        rankSum = 0.0
        for a in answerList:
            if a.isPos:
                rankSum += a.rank
        return 1.0 - (rankSum - optimumRankSum)/npairs
        
####################  main

if __name__ == "__main__":

    metrics = {'mrr':MeanRecipRank(), 'recall':Recall(), 'map':MeanAvgPrecision(), 'auc':AreaUnderROC()}

    argspec = ["data=", "answers=", "metric=", "help", "debug"]
    optlist,remainingArgs = getopt.getopt(sys.argv[1:], "", argspec)
    option = dict(optlist)
    if ('--data' not in option) or ('--answers' not in option):
        print 'usage:',sys.argv[0],'--data TRAIN-OR-TEST-FILE --answers ANSWER-FILE --metric M'
        for key in metrics:
            print '  --metric',key,metrics[key].explanation()
        sys.exit(-1)

    labels = Labels(option['--data'])
    answers = Answers(option['--answers'],labels)

    if '--debug' in option:
        print 'labels:'
        labels.show()
        print 'answers:'
        answers.show(summary=False)

    for (key,val) in optlist:
        if key=='--metric':
            if val in metrics:
                print '=' * 78
                print 'metric %s %s' % (val,metrics[val].explanation())
                print '. micro:',metrics[val].microAverage(answers,labels)
                print '. macro:',metrics[val].macroAverage(answers,labels)
                print '. details:'
                d = metrics[val].detailedReportAsDict(answers,labels)
                for q in d:
                    print '. . ',q,'\t',d[q]
            else:
                print 'unknown metric',val,'use --help for help'
