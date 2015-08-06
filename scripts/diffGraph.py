#!/usr/bin/python

import sys
from os import write

def readGraph(filename):
    write(2, "reading '%s'\n" % filename)
    features = [""]
    with open(filename+".features",'r') as flist:
        for f in flist:
            features.append(f.strip())
    with open(filename,'r') as grounded:
        for g in grounded:
            edges = {}
            g = g.replace("\n","")
            (query,qid,pid,nid,nn,ne,nld,graph) = g.split("\t",7)
            for e in range(1,int(nn)+1):
                edges[str(e)] = {}
            for e in graph.split("\t"):
                (ns,fs) = e.split(":")
                (src,dst) = ns.split("->")
                # take f up to @ if f has an @, to end of f otherwise
                fs = "\\n".join([features[int(f[0:(f.find("@"),len(f))[f.find("@")<0]])+(0,1)[f.find("@")<0]] for f in fs.split(",")])
                if fs.find("id(restart)") + fs.find("id(defaultRestart)") + 1 >= 0: continue
                edges[src][fs] = dst
                if dst not in edges:
                    edges[dst] = {}
                #fs = "\\n".join([features[int(f[0:f.find("@")])] for f in fs.split(",")])
            return (qid,edges)

def step(cursorA, graphA, cursorB, graphB, knownA={'1':()}, knownB={'1':'1'}):
    done = {}
    if cursorA is not None:
        for labelA in graphA[cursorA].keys():
            done[labelA]=()
            dstA = graphA[cursorA][labelA]
            if cursorB is not None and labelA in graphB[cursorB]:
                dstB = graphB[cursorB][labelA]
                print "%s -> %s [label=\"%s\"];" % (cursorA, dstA, labelA)
                if dstA not in knownA:
                    print "%s [label=\"%s/%s\"];" % (dstA,dstA,dstB)
                    knownA[dstA] = ()
                    knownB[dstB] = dstA
                    step(dstA, graphA, dstB, graphB, knownA, knownB)
            else:
                print "%s -> %s [label=\"%s / NONE\",color=red,fontcolor=red];" % (cursorA,dstA,labelA)
                if dstA not in knownA:
                    print "%s [label=\"%s/NONE\",color=red,fontcolor=red];" % (dstA,dstA)
                    knownA[dstA] = ()
                    step(dstA, graphA, None, graphB, knownA, knownB)
    if cursorB is None: return
    for labelB in graphB[cursorB].keys():
        if labelB in done: continue
        dstB = graphB[cursorB][labelB]
        doStep = False
        if dstB not in knownB:
            knownB[dstB] = "X%s" % dstB
            print "%s [label=\"NONE/%s\",color=orange,fontcolor=orange];" % (knownB[dstB],dstB)
            doStep=True
        print "%s -> %s [label=\"NONE / %s\",color=orange,fontcolor=orange];" % (knownB[cursorB], knownB[dstB], labelB)
        if doStep: step(None, graphA, dstB, graphB, knownA, knownB)

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print "Usage:\n\t$ python %s queriesA.grounded queriesB.grounded > diffGraph.dot" % sys.argv[0]
        exit(0)
    
    
    (cursorA,graphA) = readGraph(sys.argv[1])
    print graphA
    (cursorB,graphB) = readGraph(sys.argv[2])
    print graphB
    
    print "digraph G {"
    step(cursorA, graphA, cursorB, graphB)
    print "}"

    
    
