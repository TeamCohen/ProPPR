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
	if len(sys.argv) < 4:
		print "Usage:\n\t$ python %s queries.grounded queries.key queries.em > graph.dot" % sys.argv[0]
		print "\nGenerate .key files by specifying '--graphKey queries.key' during grounding"
		print "\nGenerate .em files with TransitionGenerator"
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
	# read the transition matrix (for correct squashing)
	em = {}
	N=1
	total = 0
	with open(sys.argv[3],'r') as emData:
		for e in emData:
			if N>1:
				write(2, "No multi-query support yet :(\n")
				break
			e = e.replace("\n","")
			(eid,nrows,rest) = e.split("\t",2)
			rest = rest.split("\t")
			for i in range(int(nrows)):
				uid = str(i+1)
				if uid not in em: em[uid] = {}
				degu = int(rest[0])
				rest = rest[1:]
				for xvi in range(degu):
					(vid,wt) = rest[xvi].split(" ")
					em[uid][vid] = float(wt)
					total += em[uid][vid]
				rest = rest[degu:]

	
	# walk the graph and accumulate paths
	debug = False
	solutions = []
	for t,m in meta.iteritems():
		if isPosP(m) or isNegP(m): solutions.append(t)
	write(2, "%d solutions\n" % len(solutions))
	edges = {}
	for t in solutions:
		for P in walk(t,igraph,meta,{}):
			v=False
			for u in P:
				if v:
					edges[(u,v)] = em[u][v]
				v = u
	edgeList = sorted(edges.items(),key=lambda (k,v):-v)
	requiredNodes = set(solutions + ['1'])
	spanningTree = {}
	units = {}
	inunit = {}
	unitid=0
	def initNode(u):
		if u not in spanningTree:
			spanningTree[u] = []
			if u in requiredNodes: requiredNodes.remove(u)
	def getUnit(u):
		if u in units:
			return units[u]
		return -1
	T=0
	debug = True
	while len(requiredNodes)>0 or len(inunit)!=1:
		if len(edgeList)==0: break
		T += 1
		nadd=0
		for e in edgeList:
			(u,v) = e[0]
			uu = getUnit(u)
			uv = getUnit(v)
			#if uu == uv and uu != -1: continue # skip cycles
			if uu == uv:
				if uu == -1:
					if len(requiredNodes)==0:continue #stop adding new components once we've hit all the targets
					# add a new component
					unitid += 1
					units[u] = unitid
					units[v] = unitid
					inunit[unitid] = [u,v]
					if debug: print "# new component %d with %s" % (unitid,inunit[unitid])
				elif debug: print "# adding cycle to unit %d %s -> %s" % (uu,u,v)
			else:
				if uu == -1:
					#if u not in requiredNodes: continue # don't add new parents, only new children
					units[u] = uv
					inunit[uv].append(u)
					if debug: print "# added %s to unit %d" % (u,uv)
				elif uv == -1:
					units[v] = uu
					inunit[uu].append(v)
					if debug: print "# added %s to unit %d" % (v,uu)
				else:
					# merge two existing components into the bigger one
					(refu,mergeu) = (uu,uv)
					if len(inunit[uu]) < len(inunit[uv]): (refu,mergeu) = (uv,uu)
					if debug: print "# merged %d into %d %s" % (mergeu,refu,inunit[mergeu])
					for x in inunit[mergeu]:
						units[x] = refu
						inunit[refu].append(x)
					del inunit[mergeu]
			initNode(u)
			initNode(v)
			spanningTree[u].append(v)
			nadd+=1
			edgeList.remove(e)
			if debug: print "# %d edges, %d targets remaining, %d components" % (len(edgeList),len(requiredNodes),len(inunit))
			if len(requiredNodes)==0 and len(inunit)==1: break
		if nadd==0: 
			if debug: print "# no more valid edges %s" % inunit
			break
		#if T>10: break
	
	displayGraph = {}
	def makeDisplayGraph(tree,displayGraph,u='1'):
		if u in displayGraph: return
		displayGraph[u] = []
		for v in tree[u]:
			displayGraph[u].append(v)
			makeDisplayGraph(tree,displayGraph,v)
	makeDisplayGraph(spanningTree,displayGraph)
	
	rlist = []
	first=True
	while first or len(rlist)>0:
		first = False
		rlist = []
		for k,v in displayGraph.iteritems():
			if len(v)==0 and k not in solutions: 
				rlist.append(k)
		for k in rlist: del displayGraph[k]
		for k,v in displayGraph.iteritems():
			displayGraph[k] = filter(lambda x:x in displayGraph,v)
	
	ndec = set()
	edec = set()
	print "digraph G {"
	print "node [shape=record];"
	print "graph [concentrate=true,ranksep=1,nodesep=1];"
	for k in displayGraph:
		declareNode(k,meta)
		ndec.add(k)
	for k,n in displayGraph.iteritems():
		for v in n:
			print "%s -> %s [label=\" %s\"];" % (k,v,igraph[v][k][1])
			edec.add( (k,v) )
	#print "node [color=gray80,fontcolor=gray80];"
	#print "edge [color=gray80,fontcolor=gray80];"
	#for e in edgeList:
	#	(u,v) = e[0]
	#	if u not in ndec: 
	#		declareNode(u,meta)
	#		ndec.add(u)
	#	if v not in ndec:
	#		declareNode(v,meta)
	#		ndec.add(v)
	#	if (u,v) not in edec:
	#		print "%s -> %s [label=\" %s\"];" % (u,v,igraph[v][u][1])
	print "}"
	
