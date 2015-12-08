# old code calls Make to build gradents, etc
# seems way to complicated....

import sys
import re
import random
import logging
import collections


def lift(src,dst):
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
    
def lower(src,dst):
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

def relationsToExamples(src,dst):
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

def gradientToRules(src,dst):
    rules = []
    for line in open(src):
        if not line.startswith("#"):
            (feature,weightStr) = line.strip().split("\t")
            weight = float(weightStr)
            if weight<0:
                parts = filter(lambda x:x, re.split('\W+', feature))
                print "feature",feature,'parts',parts
                if len(parts)==3:
                    (iftype,p,q) = parts
                    if iftype=='if':
                        rules.append( "learnedPred(%s,X,Y) :- learnedPred(%s,X,Y)." % (p,q))
                    elif iftype=='ifInv':
                        rules.append( "learnedPred(%s,X,Y) :- learnedPred(%s,Y,X)." % (p,q))
                elif len(parts)==4:
                    (chaintype,p,q,r) = parts                
                    if chaintype=='chain':
                        rules.append( "learnedPred(%s,X,Y) :- learnedPred(%s,X,Z), learnedPred(%s,Z,Y)." % (p,q,r))
    fp = open(dst,'w')
    fp.write("\n".join(rules) + "\n")

if __name__=="__main__":
    logging.basicConfig(level=logging.INFO)
    subcommand = sys.argv[1]
    if subcommand=='lift':
        lift(sys.argv[2],sys.argv[3])
    elif subcommand=='lower':
        lower(sys.argv[2],sys.argv[3])
    elif subcommand=='rel2ex':
        #TODO options
        relationsToExamples(sys.argv[2],sys.argv[3])
    elif subcommand=='grad2ppr':
        #TODO options
        gradientToRules(sys.argv[2],sys.argv[3])
    else:
        assert False,'does not compute '+subcommand
