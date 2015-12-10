#!/usr/bin/python

import sys
from os import write
from os import path
import groundUtils

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
				(query, state_id, state_version, (state_metadata), isQuery, isCompleted) = groundUtils.parseGraphKeyLine(k)
				if len(Q)>0 and Q != query:
					break
				if state_version is groundUtils.VERSION_2WAM:
					(heap,reg,rest,calls) = state_metadata
					if isCompleted or isQuery:
						print "%s [fillcolor=gray,style=filled,label=\"%s| %s] %s] %s\\lcallstack:%s\\l\"];" % (state_id,state_id,heap,reg,rest,calls)
					else:
						print "%s [label=\"%s| %s] %s] %s\\lcallstack:%s\\l\"];" % (state_id,state_id,heap,reg,rest,calls)
				elif state_version is groundUtils.VERSION_2CANON:
					canon = state_metadata.replace(",","\\l")
					if isCompleted or isQuery:
						print "%s [fillcolor=gray,style=filled,label=\"%s| %s\\l\"];" % (state_id,state_id,canon)
					else:
						print "%s [label=\"%s| %s\\l\"];" % (state_id,state_id,canon)
				elif state_version is groundUtils.VERSION_1:
					(head,tail)	= state_metadata
					if isCompleted or isQuery:
						print "%s [fillcolor=gray,style=filled,label=\"%s| %s\\l\"];" % (state_id,state_id,head)
					else:
						print "%s [label=\"%s| %s ...\\l%s\\l\"];" % (state_id,state_id,head,tail)
				Q = query
	masterFeatures=groundUtils.fetchMasterFeatures(sys.argv[1])
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
				if len(p)<1: continue
				print "%s [fillcolor=green,style=filled];"%p
			for n in nid.split(","):
				if len(n)<1: continue
				print "%s [fillcolor=red,style=filled];"%n
			for e in graph.split("\t"):
				(ns,fs) = e.split(":")
				(src,dst) = ns.split("->")
				fs = "\\n".join([features[int(f[0:(f.find("@"),len(f))[f.find("@")<0]])+(0,1)[f.find("@")<0]] for f in fs.split(",")])
				if fs == "id(restart)": continue
				if fs == "id(trueLoop)": continue
				print "%s -> %s [label=\" %s\"];" % (src,dst,fs)
			N+=1
	print "}"
