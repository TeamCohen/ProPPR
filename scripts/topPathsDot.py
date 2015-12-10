#!/usr/bin/python

import sys
from os import write
import groundUtils
from math import exp

# metadata format:
# (query, state id, state version, metadata, isquery, iscompleted, ispos, isneg)
def getData(m):
	return m[3]
def isQueryP(m):
	return m[4]
def isCompletedP(m):
	return m[5]
def isPosP(m):
	return m[6]
def isNegP(m):
	return m[7]

def walk(cursor, igraph, meta, table, path=[], depth=0):
	"""
	walk backwards from the solution to a query node. this eliminates spur trips.
	
	track previously seen prefixes in a table and don't walk them again.
	"""
	debug=False
	if isQueryP(meta[cursor]): 
		if debug: print "Base case: Query node %s at depth %d" % (cursor,depth)
		return [[cursor]]
	if depth>900: 
		print "Depth exceeded"
		exit(0)
	ret = []
	for parent in igraph[cursor].iterkeys():
		if parent in path:
			if debug: print "Skipping %s loop parent %s" % (cursor,parent)
			continue
		if debug: print "Adding %s parent %s" % (cursor, parent)
		if parent not in table:
			table[parent] = walk(parent, igraph, meta, table, path+[cursor], depth+1)
			if debug: print "Tabulating %s with %s" % (parent, "\n\t".join([str(x) for x in table[parent]]))
		else:
			if debug: print "Table contains %s" % parent
		ret.extend( [[cursor]+d for d in table[parent]] )
	return ret

# this is really inefficient to do on the reversed index
# but we're only doing it for the handful of success paths
def outweight(node,igraph):
	wt = 0.0
	for k,v in igraph.iteritems():
		if node in v: wt += v[node][0]
	return wt

def weight(path, igraph, meta):
	i=1
	wt = 1.0
	while i<len(path):
		z = outweight(path[-i],igraph)
		x = igraph[path[-(i+1)]][path[-i]][0]
		# we're ignoring reset weight for now
		wt = wt * x / z
		i+=1
	return wt

def declareNode(node,meta):
	debug = False
	if debug: print "# %s" % str(meta[node])
	print "%s [" % node,
	if isCompletedP(meta[node]):
		print "style=filled,",
		if isPosP(meta[node]): print "fillcolor=green,",
		elif isNegP(meta[node]): print "fillcolor=red,",
		else: print "fillcolor=blue,",
	elif isQueryP(meta[node]): print "style=filled,fillcolor=gray,",
	print "label=\"%s| %s\\l\"];" % (node,getData(meta[node]).replace(",","\\l"))

if __name__=='__main__':
	if len(sys.argv) < 2:
		print "Usage:\n\t$ python %s queries.grounded queries.key > graph.dot" % sys.argv[0]
		print "\nGenerate .key files by specifying '--graphKey queries.key' during grounding"
		print "\nSmall/medium .dot files can then be exported to .png using graphViz (http://www.graphviz.org/):"
		print "\t$ dot -Tpng graph.dot > graph.png"
		print "\nAny size .dot file can be viewed directly using ZGRViewer (http://sourceforge.net/projects/zvtm/files/zgrviewer/) (also requires graphViz)"
		exit(0)
	
	# load the graph key
	meta = {}
	Q=""
	if len(sys.argv)>2:
		with open(sys.argv[2],'r') as key:
			for k in key:
				parsed = groundUtils.parseGraphKeyLine(k)
				(query, state_id, state_version, (state_metadata), isQuery, isCompleted) = parsed
				if len(Q)>0 and Q != query:
					break
				meta[state_id] = list(parsed + (False, False))
				Q = query
				
	# load the graph
	debug=False
	masterFeatures = groundUtils.fetchMasterFeatures(sys.argv[1])
	for i in range(len(masterFeatures)):
		if masterFeatures[i].startswith("db("):
			masterFeatures[i] = masterFeatures[i][masterFeatures[i].rfind(",")+1:-1]
	igraph = {}
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
				meta[q][4] = True
			for p in pid.split(","):
				if len(p)<1: continue
				meta[p][-2] = True
				if debug: print "# %s" % str(meta[p])
			for n in nid.split(","):
				if len(n)<1: continue
				meta[n][-1] = True
				if debug: print "# %s" % str(meta[n])
			for e in graph.split("\t"):
				(ns,fs) = e.split(":")
				labels=""
				wt=0.0
				for f in fs.split(","):
					fw = 1.0
					fid = -1
					x = f.find("@")
					if x>0: 
						fw = float(f[(x+1):])
						fid = int(f[:x])
					else:
						fid = int(f)+1
					labels = "\\n".join((labels,features[fid]))
					wt += fw
				labels = labels[2:]
				if labels == "id(restart)": continue
				if labels == "id(trueLoop)": continue
				(src,dst) = ns.split("->")
				#if src not in graph: graph[src] = {}
				if dst not in igraph: igraph[dst] = {}
				#if dst in graph[src]:
				#	os.write(2,"duplicate edge specification %s->%s" % (src,dst))
				if src in igraph[dst]:
					write(2,"duplicate edge specification %s->%s" % (src,dst))
				#graph[src][dst] = (wt,labels)
				igraph[dst][src] = (wt,labels)
			N+=1
	debug = False
	if debug: 
		print "Graph:"
		for k,v in igraph.iteritems():
			print "\t%s -> %s" % (k,v)
		print
	
	# walk the graph and accumulate paths
	debug = False
	solutions = []
	for s,m in meta.iteritems():
		if isPosP(m) or isNegP(m): solutions.append(s)
	scores = []
	for s in solutions:
		solscores = []
		ret = walk(s,igraph,meta,{})
		for x in ret:
			solscores.append( (x,weight(x,igraph,meta)) )
		solscores = sorted(solscores,key=lambda e:-e[1])
		scores.extend(solscores[0:min(len(solscores)-1,3)])
	scores = sorted(scores,key=lambda e:-e[1])
	if debug: 
		print "\n\nComplete:"
		for x in scores:
			print "%g\t%s" % (x[1],x[0])
	print "digraph G {"
	print "node [shape=record];"
	print "graph [concentrate=true,ranksep=1,nodesep=1];"
	declared={}
	edeclared={}
	maxcolor = 85
	maxpen = 4.0
	colors = [ int(float(x)/len(scores)*maxcolor) for x in range(len(scores)) ]
	for path in scores:
		child=""
		# display path weight using pen color & line weight
		cstr = "color=gray%d,fontcolor=gray%d" % (colors[0],colors[0])
		print "node [%s];" % cstr
		print "edge [%s,penwidth=%g];" % (cstr,maxpen*exp(-float(colors[0])/maxcolor*maxpen))
		colors=colors[1:]
		for node in path[0]:
			if node not in declared:
				declared[node]=True
				declareNode(node,meta)
			if child != "":
				e = "%s -> %s" % (node,child)
				if e not in edeclared:
					edeclared[e]=True
					print "%s [label=\" %s\"];" % (e,igraph[child][node][1])
			child = node
		p = path[0]
		p.reverse()
		write(2,"%g\t%s\n" % (path[1],p))
	print "}"
