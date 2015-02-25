#!/usr/bin/python

import sys
from os import write

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print "Usage:\n\t$ python %s queries.grounded queries.key > graph.dot" % sys.argv[0]
        print "\nGenerate .key files by specifying '--graphKey queries.key' during grounding"
        print "\nSmall/medium .dot files can then be exported to .png using graphViz (http://www.graphviz.org/):"
        print "\t$ dot -Tpng graph.dot > graph.png"
        print "\nAny size .dot file can be viewed directly using ZGRViewer (http://sourceforge.net/projects/zvtm/files/zgrviewer/) (also requires graphViz)"
        exit(0)
    print "digraph G {"
    print "node [shape=record];"
    Q=""
    with open(sys.argv[2],'r') as key:
        for k in key:
            k = k.replace("\n","")
            (query,id,state) = k.split("\t")
            if len(Q)>0 and Q != query:
                break
            state = state.replace("state<","")
            state = state.replace(">","")
            (heap,reg,calls,rest) = state.split("] ")
            calls = calls.replace("c[","").replace("sf:","\\lsf:")
            if "*" in rest or id=="1":
                print "%s [fillcolor=gray,style=filled,label=\"%s| %s] %s] %s\\lcallstack:%s\\l\"];" % (id,id,heap,reg,rest,calls)
            else:
                print "%s [label=\"%s| %s] %s] %s\\lcallstack:%s\\l\"];" % (id,id,heap,reg,rest,calls)
            Q = query
                
    N=1;
    with open(sys.argv[1],'r') as grounded:
        for g in grounded:
            if N>1:
                write(2, "No multi-query support yet :(\n")
                break
            g = g.replace("\n","")
            (query,qid,pid,nid,nn,ne,flist,graph) = g.split("\t",7)
            features = [""]
            features.extend(flist.split(":"))
            for e in graph.split("\t"):
                (ns,fs) = e.split(":")
                (src,dst) = ns.split("->")
                fs = "\\n".join([features[int(f[0:f.find("@")])] for f in fs.split(",")])
                if fs == "id(restart)": continue
                print "%s -> %s [label=\" %s\"];" % (src,dst,fs)
            N+=1
    print "}"
