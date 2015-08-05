#!/usr/bin/python

import sys
from os import write
from os import path

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print "Usage:\n\t$ python %s queries.grounded [queries.key] > graph.dot" % sys.argv[0]
        print "\nGenerate .key files by specifying '--graphKey queries.key' during grounding"
        print "\nSmall/medium .dot files can then be exported to .png using graphViz (http://www.graphviz.org/):"
        print "\t$ dot -Tpng graph.dot > graph.png"
        print "\nAny size .dot file can be viewed directly using ZGRViewer (http://sourceforge.net/projects/zvtm/files/zgrviewer/) (also requires graphViz)"
        exit(0)
    print "digraph G {"
    print "node [shape=record];"
    Q=""
    if len(sys.argv)>2:
        with open(sys.argv[2],'r') as key:
            for k in key:
                k = k.replace("\n","")
                (query,id,state) = k.split("\t")
                if len(Q)>0 and Q != query:
                    break
                if state.startswith("state"): # 2.0 notation
                    state = state.replace("state<","")
                    state = state.replace(">","")
                    (heap,reg,calls,rest) = state.split("] ")
                    calls = calls.replace("c[","").replace("sf:","\\lsf:")
                    if "*" in rest or id=="1":
                        print "%s [fillcolor=gray,style=filled,label=\"%s| %s] %s] %s\\lcallstack:%s\\l\"];" % (id,id,heap,reg,rest,calls)
                    else:
                        print "%s [label=\"%s| %s] %s] %s\\lcallstack:%s\\l\"];" % (id,id,heap,reg,rest,calls)
                elif state.startswith("lpState"): # 1.0 notation
                    state = state.replace("lpState: ","")
                    state = state.replace("c[","")
                    state = state.replace("v[-","X")
                    state = state.replace("]","")
                    (head,tail) = state.split(" ... ")
                    tail = tail.replace(" ","\\l")
                    if tail is "" or id is "1":
                        print "%s [fillcolor=gray,style=filled,label=\"%s| %s\\l\"];" % (id,id,head)
                    else:
                        print "%s [label=\"%s| %s ...\\l%s\\l\"];" % (id,id,head,tail)
                else:
                    write(2, "Didn't recognize key file syntax :(\n")
                Q = query
    masterFeatures=[""]
    masterFeaturesFile = "%s.features"%sys.argv[1]
    if path.isfile(masterFeaturesFile):
    	with open(masterFeaturesFile,'r') as featureFile:
    		for f in featureFile:
    			masterFeatures.append(f.strip())
    N=1;
    with open(sys.argv[1],'r') as grounded:
        for g in grounded:
            if N>1:
                write(2, "No multi-query support yet :(\n")
                break
            g = g.replace("\n","")
            (query,qid,pid,nid,nn,ne,flist,graph) = g.split("\t",7)
            features = [""]
            if flist.find(":")>=0:
            	features.extend(flist.split(":"))
            else:
            	features = masterFeatures
            for q in qid.split(","):
                print "%s [fillcolor=gray,style=filled];"%q
            for p in pid.split(","):
                print "%s [fillcolor=green,style=filled];"%p
            for n in nid.split(","):
                print "%s [fillcolor=red,style=filled];"%n
            for e in graph.split("\t"):
                (ns,fs) = e.split(":")
                (src,dst) = ns.split("->")
                fs = "\\n".join([features[int(f[0:(f.find("@"),len(f))[f.find("@")<0]])+(0,1)[f.find("@")<0]] for f in fs.split(",")])
                if fs == "id(restart)": continue
                print "%s -> %s [label=\" %s\"];" % (src,dst,fs)
            N+=1
    print "}"
