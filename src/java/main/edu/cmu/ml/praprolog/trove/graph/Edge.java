package edu.cmu.ml.praprolog.trove.graph;

/**
 * Encodes a directed edge suitable for use as a key.
 * @author krivard
 *
 */
public class Edge implements Comparable<Edge> {
	public final int uid,vid,hash;
	public Edge(int u, int v) { uid = u; vid = v; hash = (uid << 16) ^  vid; }
	public int hashCode() { return hash; }
	@Override
	public int compareTo(Edge o) {
		// this is a bit odd but works
		return hashCode() - o.hashCode();
	}
	public String toString() {
		return uid+":"+vid;
	}
	@Override
	public boolean equals(Object o) {
		// the intuitive version:
//		if (o instanceof Edge) {
//			Edge e = (Edge) o;
//			return e.uid.equals(uid) && e.vid.equals(vid);
//		}
//		return false;
		// the guaranteed-compare-consistent version:
		if (o instanceof Edge) {
			return compareTo((Edge)o) == 0;
		}
		return false;
	}
}