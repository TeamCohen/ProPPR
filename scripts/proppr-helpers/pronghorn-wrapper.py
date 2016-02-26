import sys
import os
import shutil
import getopt
import logging
import subprocess
import util as u

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

if __name__=="__main__":
    logging.basicConfig(level=logging.INFO)

    #usage: the following arguments, followed by a "+" and a list 
    #of any remaining arguments to pass back to calls of the 'proppr'
    #script in invokeProppr
    argspec = ["src=", "src2=", "dst=", "dst2=", "stem=",
               "C=", "n", #global proppr opts
               "model=", "numIters=", 
    ]
    try:
        optlist,args = getopt.getopt(sys.argv[1:], 'x', argspec)
    except getopt.GetoptError as err:
        print 'option error: ',str(err)
        sys.exit(-1)
    optdict = dict(optlist)
    optdict['PROPPR_ARGS'] = args[1:]

    queries = optdict['--src']
    dbFile = optdict['--src2']
    modelFile = optdict['--dst']
    paramsFile = optdict['--dst2']
    stem = optdict['--stem']
    modelType = optdict['--model']
    numIters = int(optdict['--numIters'])
    # make ground file
    groundFile = stem+".grounded"
    u.invokeProppr(optdict,'ground',queries,groundFile)
    # make gradient file
    gradFile = stem+".gradient"
    u.invokeProppr(optdict,'gradient',groundFile,gradFile,"--epochs","0")
    # TODO: implement choosable eta
    eta = 1.0
    for i in range(numIters):
        logging.info('training pass %i' % i)
        # update pronghorn model
        u.invokeHelper(optdict,'pronghorn.py',"update",gradFile,paramsFile,dbFile,modelFile,modelType)
        # backup paramsFile
        backup = makebackup(paramsFile)
        if "--n" not in optdict:
            shutil.copyfile(paramsFile,backup)
        # proppr update
        u.invokeProppr(optdict,'gradient',groundFile,gradFile,"--epochs","1","--initParams",backup,"--params",paramsFile,"--srw","ppr:eta=%g" % eta)
        eta = eta * 0.8
    
    # update pronghorn model
    u.invokeHelper(optdict,'pronghorn.py',"update",gradFile,paramsFile,dbFile,modelFile,modelType)

