#!/usr/bin/python
import sys
import re

def gradient_to_rules(gradFile):
    rules = []
    with open(gradFile,"r") as f:
        for line in f:
            if line.startswith("#"): continue
            (sfeature,sscore) = line.strip().split("\t")
            score = float(sscore)
            if score>=0: continue
            feature = re.split("[(),]",sfeature)[:-1]
            if feature[0] == "if":
                newrule = "interp0(%s,X,Y) :- rel(%s,X,Y) {%s}." % (feature[1],feature[2],sfeature)
            elif feature[0] == "ifInv":
                newrule = "interp0(%s,X,Y) :- rel(%s,Y,X) {%s}." % (feature[1],feature[2],sfeature)
            elif feature[0] == "chain":
                newrule = "interp0(%s,X,Y) :- rel(%s,X,Z),rel(%s,Z,Y) {%s}." % (feature[1],feature[2],feature[3],sfeature)
            else:
                print "#?? unparsed feature",feature
                continue
            rules.append(newrule)
    return rules

if __name__=='__main__':
    print "\n".join(gradient_to_rules(sys.argv[1]))
