package edu.cmu.ml.praprolog.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;

/**
 * 
 * Lightweight dict-of-dicts implementation of a weighted graph which supports random walk
 * @author wcohen,krivard
 *
 */
public abstract class AnyKeyGraph<K> {
	private static final Logger log = Logger.getLogger(AnyKeyGraph.class);
	protected static final double DEFAULT_EDGEWEIGHT=1.0;
	protected static final double DEFAULT_GAMMA=0.5;
	protected static final int DEFAULT_MAXT=10;
	protected Map<K,Map<K,Double>> near;
	protected Map<K,Double> total;
	protected int numEdges=0;
	
	public abstract K keyToId(String key);
	public abstract K[] keyArray(int length);
	public abstract String idToKey(K id);
	
	public K[] keyToId(String[] key) {
		K[] result = keyArray(key.length);
		for (int i=0; i<key.length; i++) {
			result[i] = keyToId(key[i]);
		}
		return result;
	}
	
//	public AnyKeyGraph() {
//		near = new TreeMap<String,Map<String,Double>>();
//		total = new TreeMap<String,Double>();
//	}
	
	/**
	 * Add edge to the graph with default weight
	 * @param uid
	 * @param vid
	 */
	public void addDirectedEdge(String uid, String vid) { addDirectedEdge(uid,vid,DEFAULT_EDGEWEIGHT); }
	/**
	 * Add edge to the graph with specified weight
	 * @param uid
	 * @param vid
	 * @param weight
	 */
	public void addDirectedEdge(String uid, String vid, double weight) {
		K u=keyToId(uid);
		K v=keyToId(vid);
		//if(near.containsKey(u) && near.get(u).containsKey(v)) numEdges--;
		Dictionary.increment(total,u,weight);
		Dictionary.increment(near,u,v,weight);
		numEdges++;
	}
	
	/**
	 * Add edge to the graph (in both directions) with specified weight
	 * @param uid
	 * @param vid
	 * @param weight
	 */
	public void addEdge(String uid, String vid, double weight) {
		addDirectedEdge(uid, vid, weight);
		addDirectedEdge(vid, uid, weight);
		numEdges--; // only add one edge for 2 directedEdge calls
	}
	/**
	 * Random walk with restart from start vector
	 * @param startVec String->Double map of graph nodes serving as the query vector
	 * @return String->Double map of graph nodes serving as the rwr result
	 */
	public Map<K,Double> rwr(Map<K,Double> startVec) {
		return rwr(startVec, DEFAULT_GAMMA, DEFAULT_MAXT);
	}
	/**
	 * Random walk with restart from start vector using maxT iterations.
	 * @param startVec String->Double map of graph nodes serving as the query vector
	 * @param gamma relaxation constant
	 * @param maxT number of iterations
	 * @return String->Double map of graph nodes serving as the rwr result
	 */
	public Map<K,Double> rwr(Map<K,Double> startVec, double gamma, int maxT) {
		Map<K,Double> vec = startVec;
		for (int i=0; i<maxT; i++) {
			if (log.isDebugEnabled()) log.debug("iteration "+(i+1)+" of "+maxT);
			vec = walkOnce(startVec,vec,gamma);
		}
		return vec;
	}
	/**
	 * one iteration of random walk with restart
	 * @param vec0 String->Double map of graph nodes serving as query vector
	 * @param vec String->Double map of graph nodes serving as current vector
	 * @param gamma relaxation constant
	 * @return String->Double map of next vector
	 */
	public Map<K,Double> walkOnce(Map<K,Double> vec0,Map<K,Double> vec,double gamma) {
		return walkOnce(vec0,vec,gamma,10000,"Propagated from %d nodes");
	}
	/**
	 * one iteration of random walk with restart
	 * @param vec0 String->Double map of graph nodes serving as query vector
	 * @param vec String->Double map of graph nodes serving as current vector
	 * @param gamma relaxation constant
	 * @param interval Print msg after processing every interval nodes
	 * @param msg Print msg after processing every interval nodes
	 * @return String->Double map of next vector
	 */
	public Map<K,Double> walkOnce(Map<K,Double> vec0,Map<K,Double> vec,double gamma,double interval, String msg) {
		Map<K,Double> nextVec = new TreeMap<K,Double>();
		for (Map.Entry<K, Double>uid : vec0.entrySet()) {
//			K uid = e.getKey();
			Dictionary.increment(nextVec,uid.getKey(),gamma * uid.getValue());
		}
		int k=0;
		for(Map.Entry<K, Double>v : vec.entrySet()) { k++; if ( (k%interval) == 0) System.err.println(String.format(msg,k));
//			K v = e.getKey();
			if (near.containsKey(v.getKey())) {
				double z = total.get(v.getKey());
				for (Map.Entry<K,Double> item : near.get(v.getKey()).entrySet()) {
					K u = item.getKey();
					double w_vu = item.getValue();
					Dictionary.increment(nextVec, u, (1.0 - gamma)*v.getValue()*w_vu/z);
				}
			}
		}
		return nextVec;
	}
	
	public Map<K,Double> near(String u) {
		return nearNative(keyToId(u));
	}

	/**
	 * Get the weighted set of outgoing connections from node u.
	 * @param u Start node
	 * @return String->Double map of weighted nodes connected to u
	 */
	public Map<K,Double> nearNative(K u) {
		Map<K,Double> ret = near.get(u);
		if (ret==null) return Collections.emptyMap();
		return ret;
	}
	
	/**
	 * Get all nodes in the graph that have outgoing edges
	 * @return Set of node names
	 */
	public Set<K> getNodes() { return near.keySet(); }
	
	public Set<String> getNodesAsStrings() {
		TreeSet<String> result = new TreeSet<String>();
		for (K id : getNodes()) {
			result.add(idToKey(id));
		}
		return result;
	}

	public int getNumEdges() { return numEdges; }
	public int getNumNodes() { return near.size(); }
	
}


/*
 * 
 *
#graph implementation that directly supports RWR and PIC

class Graph(object):
    """Lightweight dict-of-dicts implementation of a weighted graph"""
    VERBOSE = True
    DEBUG = False



    def walkOnce(self,vec0,vec,gamma=0.1,interval=10000, msg='Propogated from %d nodes'):
        """one iteration of random walk with restart"""
        nextVec = collections.defaultdict(float)
        for u in vec0:
            nextVec[u] += gamma*vec0[u]
        for k,v in enumerate(vec):
            if Graph.VERBOSE and k and (k % interval)==0: sys.stderr.write((msg % k)+"\n")
            if v in self.near:
                z = self.total[v]
                for u,w_vu in self.near[v].items():
                    nextVec[u] += (1.0 - gamma)*vec[v]*(float(w_vu)/z)
        return nextVec

    def approxRWR(self,startVec,alpha=0.5,epsilon=0.00001):
        """approximate random-walk-with-restart, from anderson, chung & lang"""
        ##NOT TESTED
        r = dict(startVec.items())
        p = {}
        #initialize the 'queue'
        qtmp = {}
        q = {}
        for x,rx in r.items():
            qx = rx/self.total[x]
            if  qx > epsilon: q[x] = qx
        while (True):
            if not len(q): break
            #remove something from the 'queue'
            u,qu = q.items()[0]
            ru = qtmp[u]*self.total[u]
            if ru<epsilon: break
            p[u] += alpha*ru
            q[u] = 0.5 * (1.0 - alpha) * ru
            if q[u]<epsilon: del q[u]
            for v in self.near[u]:
                deltaV = 0.5 * (1.0 - alpha) * ru / self.total[u] * (1.0 / self.total[v])
                qtmpV = qtmp.get(v,0.0)
                if qtmpV < epsilon and qtmpV+deltaV >= epsilon:
                    q[v] = qtmpV + epsilon
                qtmpV += deltaV
        return p

    #power iteration clustering

    def picEmbedding(self,vecStart,maxT=50,minAccel=None,interval=10000):
        """Compute a PIC embedding with the given starting vector."""
        if not minAccel: minAccel=0.00001
        deltaNow = collections.defaultdict(float)
        deltaNext = {}
        epsilon = {}
        vecNow = dict(vecStart)
        for i in xrange(maxT):
            vecNext = self.averageWithNeighbors(vecNow,interval=interval)
            util.normalizeVector(vecNext)
            for u in vecNext: 
                deltaNext[u] = vecNext[u] - vecNow[u]
                epsilon[u] = deltaNext[u] - deltaNow[u]
            accel = max(abs(x) for x in epsilon.values()) / len(self.near)

            if Graph.VERBOSE: print 'iteration',i+1,'of',maxT,'accel',accel
            if accel<minAccel:
                if Graph.VERBOSE: print 'converged - acceleration <',minAccel
                break

            #copy over values for next iteration
            for u in vecNext:
                vecNow[u] = vecNext[u]
                deltaNow[u] = deltaNext[u]
        return vecNow

    def averageWithNeighbors(self, vec, interval=100):
        nextVec = collections.defaultdict(float)
        for k,u in enumerate(self.near):
            if k and (k % interval)==0: sys.stderr.write(('averaged %d nodes' % k)+"\n")
            for v,w_uv in self.near[u].items():
                nextVec[u] += vec[v]*w_uv/self.total[u]
        return nextVec

    def _showVec(self,tag,v,chop=10): 
        if not DEBUG: return
        print tag,
        for u,x in v.items()[:chop]: print "%s:%5.3f" % (u,x),
        print

    def normalizedDegree(self, u):
        """Degree of u in the column-normalized version of the graph"""
        return sum(self.near[v][u]/self.total[v] for v in self.near[u])

    def cc(self,u,maxClose=10,sampleSize=25):
        """Clustering coefficient for node u.  If there are more than
        maxClose neighbors of u, then the clustering coefficient will
        be approximated using a sample of neighbors rather than
        computed exactly."""
        close = list(self.near[u].keys())
        triangles = 0
        if len(close)<2: return 1.0
        elif len(close)<maxClose:
            #compute clustering coefficient exactly
            for i in xrange(len(close)):
                for j in xrange(i+1,len(close)):
                    if self.near[close[i]].get(close[j]):
                        triangles += 1
            return 2.0*triangles/(len(close)*(len(close)-1))
        else:
            #approximate clustering coefficient
            r = random.Random()
            triangles = 0
            trials = 0
            for s in xrange(sampleSize):
                u = random.choice(close)
                v = random.choice(close)
                if u!=v:
                    trials += 1
                    if self.near[u].get(v):
                        triangles += 1
            return triangles/trials

                          
    def show(self):
        """Display the rows of the graph"""
        for u,row in self.near.items():
            print u,'=>',row


class FileGraphWriter(object):
    """Used for building a graph by redirecting addEdge operations to
    a a file."""
    def __init__(self,fileName):
        self.tell(fileName)
    def tell(self,fileName):
        """Start writing to the given file"""
        self._f = open(fileName,'w')
    def addEdge(self,u,v,w=1.0):
        """Write an edge to the file"""
        self._f.write("%s\t%s\t%f\n" % (util.encode(u),util.encode(v),w))
    def told(self):
        """Close the file"""
        self._f.close()
    def loadEdges(self,graphWriter,fileName):
        """Load edges previously written to a file"""
        for line in util.linesOf(fileName, msg='loaded %d edges from '+fileName):
            (u,v,w) = line.split("\t")
            graphWriter.addEdge(util.decode(u),util.decode(v),float(w))

class BasicGraphWriter(object):
    """Used for building a graph by delegating addEdge operations to
    an inner graph."""
    def __init__(self,graph):
        self._g = graph
    def addEdge(self,u,v,w=1.0):
        self._g.addEdge(u,v,w)

def _query(g,s=None,numTop=20,maxT=4,minScore=0.0005):
    """prompt for a start node, then do a RWR and print the most similar things,
    sort of like ghirl's query interface."""
    if not s: s = raw_input('start node? ')
    if g.near.get(s):
        v0 = {s: 1.0}
        v = g.rwr(v0,maxT=maxT)
        if Graph.VERBOSE: print 'sorting...'
        ordered = sorted([(w,u) for (u,w) in v.items() if w>=minScore], reverse=True)
        for (w,u) in ordered[0:numTop]: 
            print "%.3f:  %s" % (w,u)
    else:
        print 'cannot find node',s

# test code - syntax is a list of edges, each in the form u,v where u
# and v are nodes, with start nodes starting with a leading

if __name__ == "__main__":
    g = Graph()
    for line in util.linesOf(sys.argv[1], msg='loaded %d edges from '+sys.argv[1], interval=100000):
        (u,v) = line.split("\t")
        g.addEdge(u,v)
    while (True):
        _query(g)
 
 */
