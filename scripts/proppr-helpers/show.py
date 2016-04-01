import sys

#

if __name__=="__main__":
   #usage: INPUT.grounded example# --maxDepth d
   grounded = sys.argv[1]
   eid = int(sys.argv[2])
   v = [sys.argv[3],int(sys.argv[4])]
   maxDepth=-1
   if v[0] == '--maxDepth':
      maxDepth=int(v[1])
   featurefile = "%s.features" % grounded
   features = [0]
   with open(featurefile,'r') as f:
      features.extend([line.strip() for line in f])
   if "id(trueLoop)" in features:
      trueLoop = features.index("id(trueLoop)")
   else:
      trueLoop = -1
   lineno=0
   query = ""
   edges = []
   pos = []
   neg = []
   with open(grounded,'r') as f:
      for line in f:
         lineno+=1
         if lineno == eid:
            fields = line.strip().split("\t")
            query = fields[0]
            if fields[2] != '': pos = [int(n) for n in fields[2].split(',')]
            if fields[3] != '': neg = [int(n) for n in fields[3].split(',')]
            edges = fields[7:]
            break
   graph = {}
   for edge in edges:
      (e,F) = edge.split(":")
      (src,dest) = [ int(n) for n in e.split("->") ]
      if not graph.has_key(src): graph[src] = {}
      if dest==1: continue
      if not graph[src].has_key(F): graph[src][F] = []
      graph[src][F].append(dest)
      if not graph.has_key(dest): graph[dest] = {}
   posneg = pos+neg
   posneg.sort()
   
   cursor = 1
   def show(cursor,level):
      print "%d" % (cursor),
      if cursor in pos: print "+",
      elif cursor in neg: print "-",
      if type(graph[cursor]) == type(1):
         # then we've seen this node before
         graph[cursor] += 1
         print "%% (x%d)" % graph[cursor]
      else:
         descendants = graph[cursor]
         graph[cursor] = 1
         if len(descendants) > 1: print ">>"
         elif len(descendants) == 0: print "R"
         elif not descendants.keys()[0].startswith("%d@" % trueLoop):
            print ">>"
         elif cursor not in posneg:
            print "?"
         else: print ""
         for (F,links) in descendants.iteritems():
            labels = []
            skip = False
            for f in F.split(","):
               p = f.split("@")
               label = (features[int(p[0])], float(p[1]))
               if label[0] == "id(trueLoop)":
                  skip = True
               labels.append(label)
            if skip: continue
            for link in links:
               print "%s%s: " % ( '  |'*level,labels),
               if (maxDepth<0) or (level<maxDepth):
                  show(link,level+1)
               else:
                  print "%d [truncated]" % link
            #print "%s  ---" % ( '  |'*(level-1))
   print query
   show(1,1)
