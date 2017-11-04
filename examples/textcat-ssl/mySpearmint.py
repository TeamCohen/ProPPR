#!/usr/bin/python

import subprocess

_settings="settings.in"
_results="%s.results"
_log="hyperparams.log"
_steps="steps.conf"
_tmp_option="tmp_option"

def main(job_id, params):
    result = 0.0
    stepDict=loadStep()
    allOption=loadOption()
    corpus=allOption.strip().split()[0]
    otherOption=allOption.replace(corpus+" ","")

#    tmpOutFile=open("tmpOutFile",'a')

    with open(_log,'a') as logFile:
        logFile.write("#\n");
        with open(_settings,'w') as settingFile:
            for (key,val) in params.iteritems():
                newVal=timeStep(params,stepDict,key,val)
                settingFile.write("%s:=%g\n"%(key,newVal))
                logFile.write("%s\t%g\n" % (key,newVal))
            settingFile.write("job_id:=%s\n"%job_id)

        subprocess.call(("make -e "+corpus+".expt "+otherOption).split())
        
        # read results and pass it to spearmint
        with open ("one_spearmint_result",'r') as resultFile:
            for line in resultFile:
                pass
            result = -float(line.strip().split(" ")[2])
            logFile.write("acc2: "+str(result)+"\n")
#    tmpOutFile.close()
    return result

def loadOption():
    retStr=""
    with open(_tmp_option,'r') as options:
        for option in options:
            retStr=retStr+option.strip()+" "
    return retStr

def loadStep():
    retDict=dict()
    with open(_steps,'r') as stepFile:
        for line in stepFile:
            if len(line.strip().split(":"))==2:
                retDict[line.strip().split(":")[0]]=int(line.strip().split(":")[1])
    return retDict

def timeStep(params, stepDict, key, val):
    retVal=stepDict[key]*val
    return retVal
