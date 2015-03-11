import sys
import re
from subprocess import call

CALL_REL_DIRECTLY_FOR_BACKGROUND_PREDICATES = True
#MAX_ITERS = 10
MAX_ITERS = 1

RULES_EXT = ".ppr"
WAM_EXT = ".wam"
#COMPILER = "/src/scripts/compiler.py"

#################### main algorithm

def iterativeGradientFinder(stem,iters=MAX_ITERS):

    # create an empty gradient file
    gradientFile0 = ithFileName(stem,0,'.gradient')
    fp0 = open(gradientFile0,'w')
    fp0.close()
    
    gradientFiles = [gradientFile0]
    gradientFeatureSet = set()

    #iteratively find new gradient rules to add

    for i in range(iters):

        print '- starting pass ',i,'now'

        #accumulate all the gradient features into a new 2nd-order ruleset
        (ruleFile,newGradientFeatureSet) = rulesFromGradient(i+1,stem,gradientFiles)
        catfile(ruleFile,('- generated rules for pass %d' % i))

        #check convergence
        print len(newGradientFeatureSet),'gradientFeatures:',newGradientFeatureSet
        if i>0 and len(newGradientFeatureSet)==len(gradientFeatureSet):
            print '- no new features produced at iteration',i,'...halting'
            break
        else:
            gradientFeatureSet = newGradientFeatureSet
        
        nextGradientFile = ithFileName(stem,i+1,'.gradient')
        print '- gradient computed after ',i+1,'epochs'
        tcall(['make',nextGradientFile])
        gradientFiles += [nextGradientFile]

    # end of FOR loop iteration

    # create a final iterated-gradient ruleset
    final = ithFileName(stem+"_delta",iters+1,RULES_EXT)
    gradient2Rules(final,gradientFiles)
    catfile(final,'final iterated gradient')

    #create some 'final' iterated gradient (fig) rule files
    #gradient2Rules(stem+'.fig-'+RULES_EXT,gradientFiles,addSecondOrderRules=False)
    #catfile(stem+'.fig-'+RULES_EXT,'final iterated gradient with out 2nd-order rules')
    #with file(stem+'.f ig-'+WAM_EXT,'w') as f:
    #    tcall(['python',proppr+COMPILER,'serialize',stem+'.fig-'+RULES_EXT],f)

    #gradient2Rules(stem+'.fig2-'+RULES_EXT,gradientFiles,addSecondOrderRules=True)
    #catfile(stem+'.fig2-'+RULES_EXT,'final iterated gradient with 2nd-order rules')
    #with file(stem+'.fig2-'+WAM_EXT,'w') as f:
    #    tcall(['python',proppr+COMPILER,'serialize',stem+'.fig2-'+RULES_EXT],f)
    
def rulesFromGradient(i,stem,gradientFiles):
    ruleFile = ithFileName(stem+"_delta",i,RULES_EXT)
    featureSet = gradient2Rules(ruleFile,gradientFiles)
    print '-- created',ruleFile,'from',gradientFiles
    return (ruleFile,featureSet)

def catfile(fileName,msg):
    """Print out a created file - for  debugging"""
    print msg
    print '+------------------------------'
    for line in open(fileName):
        print ' |',line,
    print '+------------------------------'

def gradient2Rules(ruleFile,gradFiles):
    """Compile a list of gradient files into a rule file"""
    fp = open(ruleFile,'w')
    featureSet = set()

    def feature2rule(feat):
        
        def interpreterCall(s):
            if CALL_REL_DIRECTLY_FOR_BACKGROUND_PREDICATES:
                return 'interp0' if s.startswith('i_') else 'rel'
            else:
                return interp0

        print 'feature2rule',feat
        featureParsed = False
        m = re.match('ifInv\((\w+),(\w+)\)',feat)
        if m:
            #print '---- "ifInv" for',m.group(1),m.group(2),'detected'
            ic = interpreterCall(m.group(2))
            return 'interp0(%s,X,Y) :- %s(%s,Y,X).' % (m.group(1),ic,m.group(2))
        m = re.match('if\((\w+),(\w+)\)',feat)
        if m:
            #print '---- "if" for',m.group(1),m.group(2),'detected'
            ic = interpreterCall(m.group(2))
            return 'interp0(%s,X,Y) :- %s(%s,X,Y).' % (m.group(1),ic,m.group(2))
        m = re.match('chain\((\w+),(\w+),(\w+)\)',feat)
        if m: 
            #print '---- "chain" for',m.group(1),m.group(2),m.group(3),'detected'
            ic1 = interpreterCall(m.group(2))
            ic2 = interpreterCall(m.group(3))
            return 'interp0(%s,X,Y) :- %s(%s,X,Z),%s(%s,Z,Y).' % (m.group(1),ic1,m.group(2),ic2,m.group(3))
        return None

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

                        
    #bug - are there two copies of interp0 base case?
    fp.write('#interp0 base case\n')
    fp.write('interp0(P,X,Y) :- rel(P,X,Y) {fixedWeight}.\n')
    return featureSet


#################### file construction and command execution

def ithFileName(stem,i,extension):
    return stem+'_'+('%02d' % i)+extension

def tcall(xs,so=None): 
    """Call command in list xs, with a trace."""
    print '--calling',xs
    call(xs,stdout=so)


if __name__ == "__main__":
    (stem) = sys.argv[1]
    if len(sys.argv) > 2:
        iterativeGradientFinder(stem,int(sys.argv[2]))
    else:
        iterativeGradientFinder(stem)
