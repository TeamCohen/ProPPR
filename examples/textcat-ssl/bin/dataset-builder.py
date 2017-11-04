import sys
import re
import logging
import collections

# Micro-language: interprets how to build training data from these
# instructions, which are by convention embedded in a PPR file.  
#
##! OUTPUT: for X in LISTNAME:  +P(X)
##! OUTPUT: for X in LISTNAME:  -P(X)
##! OUTPUT: for X,Y in LISTNAME:  -P(X,Y)
##! OUTPUT: for X,Y in LISTNAME, Z in LISTNAME: +/-P(X,Z)
##! OUTPUT: for X,Y in LISTNAME, Z in LISTNAME: +/-P(Z,X)
#
# Here OUTPUT is the stem of an example file to be created in the
# output directory, ie for OUTPUT foo a file foo.examples will be
# created.  LISTNAME can be any file stem where LISTNAME.txt exists in
# the input directory. X,Y and Z can be any variable names.
#
# All lines not starting with char sequence "##!" will be ignored.

#needs some refactoring....

class DatasetBuilder(object):

    def __init__(self,inputsDir,outputsDir):
        self.inputsDir = inputsDir
        self.outputsDir = outputsDir
        self.startedFiles = set()

    def interp(self,srcFile):
        for line in open(srcFile):
            if line.startswith("##!"):
                tokens = re.findall(r'[^\w\+\-\/]*([\w\+\-\/]+)',line.strip())
                self.execCommand(line.strip(),tokens)
    
    def execCommand(self,line,toks):
        #0       1   2   3  4:    5     6
        #OUTPUT: for VAR in LISTNAME: +PRED(VAR)
        try: 
            (output,kwfor,var1,kwin,listname,signedPred,var2) = toks
            if (kwfor,kwin)==('for','in'):
                assert signedPred[0] in set(['+','-']), "ERROR: bad build-trainset line "+line+" at "+signedPred
                assert var1==var2, "ERROR: bad build-trainset line "+line+" because "+var1+"!="+var2
                self.processUnlabeled(output,listname,signedPred[0],signedPred[1:])
                return
        except ValueError:
            pass
        #0       1   2   3  4:    5     6
        #OUTPUT: for VAR1,VAR2 in LISTNAME: +PRED(VAR1,VAR2)
        try: 
            (output,kwfor,var1,var2,kwin,listname,signedPred,var3,var4) = toks
            if (kwfor,kwin)==('for','in'):
                assert signedPred[0] in set(['+','-']), "ERROR: bad build-trainset line "+line+" at "+signedPred
                assert var1==var3, "ERROR: bad build-trainset line "+line+" because "+var1+"!="+var3
                assert var2==var4, "ERROR: bad build-trainset line "+line+" because "+var2+"!="+var4
                self.processLabeledWithoutLabelList(output,listname,signedPred[0],signedPred[1:])
                return
        except ValueError:
            pass
        

        #0       1   2    3    4  5          6    7  8          9    10   11
        #OUTPUT: for VAR1,VAR2 in LISTNAME1, ZVAR in LISTNAME2: +/-P(VAR3,VAR4), ...
        try: 
            (output,kwfor,var1,var2,kwin1,listname1,zvar,kwin2,listname2,signedPred,var3,var4) = toks[0:12]
            if (kwfor,kwin1,kwin2)==('for','in','in'):
                assert signedPred[0:3]=="+/-", "ERROR: bad build-trainset line "+line+" at "+signedPred
                if (var3==var1):  # +/-predict(X,Z)
                    assert var4==zvar
                    self.processLabeled('xy',output,listname1,listname2,signedPred[3:])
                elif (var3==zvar):  # +/-predict(Z,X)
                    assert var4==var1
                    self.processLabeled('yx',output,listname1,listname2,signedPred[3:])
                return
        except ValueError:
            pass
        assert False,"ERROR: bad build-trainset line "+line.strip()
        
    def processLabeledWithoutLabelList(self,output,listname,posneg,pred):
        print '##! for X,Y in',listname+":",posneg+pred+'(X,Y) >>',output+".examples"
        mode = 'a' if output in self.startedFiles else 'w'
        self.startedFiles.add(output)
        fp = open("%s/%s.examples" % (self.outputsDir,output), mode)
        k = 0
        for line in open("%s/%s.txt" % (self.inputsDir,listname)):
            x,y = line.strip().split("\t")
            outputLine = pred+"("+x+","+y+")\t"+posneg+pred+"("+x+","+y+")"
            fp.write(outputLine+"\n")
            if k<=3: 
                print '>>',output+".examples:",outputLine
            k += 1
        if k>3: print ">> ..."
        print 'total from that command:',k,'examples for',output+".examples"

    def processUnlabeled(self,output,listname,posneg,pred):
        print '##! for X in',listname+":",posneg+pred+'(X) >>',output+".examples"
        mode = 'a' if output in self.startedFiles else 'w'
        self.startedFiles.add(output)
        fp = open("%s/%s.examples" % (self.outputsDir,output), mode)
        k = 0
        for line in open("%s/%s.txt" % (self.inputsDir,listname)):
            x = line.strip()
            outputLine = pred+"("+x+")\t"+posneg+pred+"("+x+")"
            fp.write(outputLine+"\n")
            if k<=3: 
                print '>>',output+".examples:",outputLine
            k += 1
        if k>3: print ">> ..."
        print 'total from that command:',k,'examples for',output+".examples"
    
    def processLabeled(self,order,output,labeledExampleList,labelList,pred):
        allLabels = set()
        for line in open("%s/%s.txt" % (self.inputsDir,labelList)):
            allLabels.add(line.strip())
        printableLabels = list(allLabels)
        if len(printableLabels)>3: printableLabels=printableLabels[0:3]+['...']
        print '##! for X,Y in',labeledExampleList+', Z in',(",".join(printableLabels)),\
                     ": +/-"+pred+"(X,Z), sign based on Z==Y >>",output+".examples"
        mode = 'a' if output in self.startedFiles else 'w'
        self.startedFiles.add(output)
        fp = open("%s/%s.examples" % (self.outputsDir,output), mode)
        k = 0
        if order=='xy':
            for line in open("%s/%s.txt" % (inputsDir,labeledExampleList)):
                (x,y) = line.strip().split()
                outputLine = pred+"("+x+",Y)"
                for z in allLabels:
                    posneg = "+" if y==z else "-"
                    outputLine += "\t"+posneg+pred+"("+x+","+z+")"
                fp.write(outputLine+"\n")
                if k<=3: 
                    printableOutputline = outputLine
                    if len(printableOutputline)>75: printableOutputline = printableOutputline[0:75]+'...'
                    print '>>',output+".examples:",printableOutputline
                k += 1
            if k>3: print '>> ...'
        else:
            assert order=='yx'
            for y in allLabels:
                outputLine = pred+"("+y+",X)"
                for line in open("%s/%s.txt" % (inputsDir,labeledExampleList)):
                    (x,z) = line.strip().split()
                    posneg = "+" if y==z else "-"
                    outputLine += "\t"+posneg+pred+"("+y+","+x+")"
                fp.write(outputLine+"\n")
                if k<=3: 
                    printableOutputline = outputLine
                    if len(printableOutputline)>75: printableOutputline = printableOutputline[0:75]+'...'
                    print '>>',output+".examples:",printableOutputline
                k += 1
            if k>3: print '>> ...'
        print 'total from that command:',k,'examples for',output+".examples"

if __name__=="__main__":
    try:
        srcFile = sys.argv[1]
        inputsDir = sys.argv[2]
        outputsDir = sys.argv[3]
        builder = DatasetBuilder(inputsDir,outputsDir)
        builder.interp(srcFile)
    except IndexError:
        print "usage: python",sys.argv[0],"source-file input-directory output-directory"
        print " -see header of",sys.argv[0],"for language specs for source-file"
