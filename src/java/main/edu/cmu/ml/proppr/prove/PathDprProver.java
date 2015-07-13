package edu.cmu.ml.proppr.prove;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.graph.InferenceGraph;
import edu.cmu.ml.proppr.graph.LightweightStateGraph;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.CustomConfiguration;
import edu.cmu.ml.proppr.util.Dictionary;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.custom_hash.TObjectDoubleCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

public class PathDprProver extends DprProver {
	private static final int NUMPATHS=10;//TODO: move this to command line
	protected Map<State,TopPaths> paths = new HashMap<State,TopPaths>();
	public PathDprProver(APROptions apr) {
		super(apr);
	}
	
	/**
	 * Model each path as an array of ints.
	 * 
	 * Track a new path only when <k are being tracked, or the weight of the new path is above the lowest weight in the set.
	 * 
	 * Accumulate weight if a path comes up more than once.
	 * 
	 * At end of computation, return a list of the top paths ordered by their weight.
	 * 
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	protected class TopPaths {
		// Use hashcode from Arrays instead of Object
		TObjectDoubleMap<int[]> top = new TObjectDoubleCustomHashMap<int[]>(new HashingStrategy<int[]>() {
			@Override
			public int computeHashCode(int[] arg0) {
				return Arrays.hashCode(arg0);
			}
			@Override
			public boolean equals(int[] arg0, int[] arg1) {
				return Arrays.equals(arg0, arg1);
			}
		}, NUMPATHS);
		double leastWt;
		int[] leastPath;
		/** Should we add this path? */
		public boolean qualify(double wt, int[] path) {
			return top.size() < NUMPATHS || 
					top.containsKey(path) ||
					wt > leastWt;
		}
		public void add(double wt, int[] path) {
			if (!qualify(wt,path)) return;
			top.adjustOrPutValue(path, wt, wt);
			if (top.size() > NUMPATHS) top.remove(leastPath);
			rebalance();
		}
		/** Find and record lowest-weighted path */
		private void rebalance() {
			leastWt=Double.MAX_VALUE;
			for(TObjectDoubleIterator<int[]> it = top.iterator(); it.hasNext();) {
				it.advance();
				if (it.value() < leastWt) {
					leastWt = it.value();
					leastPath = it.key();
				}				
			}
		}
		/** Return ordered list of top paths & their weights */
		public WeightedPath[] result() {
			WeightedPath[] ret = new WeightedPath[top.size()];
			int i=0;
			for(TObjectDoubleIterator<int[]> it = top.iterator(); it.hasNext();) {
				it.advance();
				ret[i++] = new WeightedPath(it.key(),it.value());
			}
			Arrays.sort(ret);
			return ret;
		}
	}
	protected int[] createPath(Backtrace<State> b) {
		int[] path = new int[b.backtrace.size()];
		int i=path.length;
		for (State s : b.backtrace) {
			if (i<path.length && s.getJumpTo() == null)
				return null;
			path[--i] = current.getId(s);
		}
		return path;
	}
	protected class WeightedPath implements Comparable<WeightedPath> {
		int[] path;
		double wt;
		public WeightedPath(int[] p, double w) {
			this.wt = w;
			this.path = p;
		}
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof WeightedPath)) return false;
			WeightedPath w = (WeightedPath) o;
			if (w.path.length != this.path.length) return false;
			for (int i=0; i<this.path.length; i++) {
				if (w.path[i] != this.path[i]) return false;
			}
			return true;
		}
		@Override
		public int compareTo(WeightedPath w) {
			return Double.compare(w.wt, this.wt);
		}
		/** Convert the array of state IDs to a string representation of state jumpto, feature, state jumpto, feature, etc **/
		public String humanReadable(StateProofGraph pg, Map<State,Double> ans) {
//			GroundedExample ex = pg.makeRWExample(ans);
			LightweightStateGraph g = pg.getGraph();
			StringBuilder sb = new StringBuilder(String.format("%+1.8f ",this.wt)).append(this.path.length).append(" ");
			for (int i=1; i<path.length; i++) {
				sb.append(g.getState(path[i-1]).getJumpTo());
				sb.append(" ->");
				for (Goal phi : g.getFeatures(g.getState(path[i-1]), g.getState(path[i])).keySet()) {
					sb.append(phi).append(",");
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append(": ");
			}
			sb.delete(sb.length()-2, sb.length()-1);
			return sb.toString();
		}
	}
	@Override
	protected void addToP(Map<State, Double> p, State u, double ru) {
		super.addToP(p, u, ru);
		double wt = ru;
		if (u.isCompleted() && !u.isFailed()) {
			// add paths to solution state to our top-paths tracker
			int[] path = createPath(this.backtrace);
			if (path == null) return;
			if (!paths.containsKey(u)) paths.put(u, new TopPaths());
			TopPaths t = paths.get(u);
			t.add(wt,path);
		}
	}

	@Override
	public Map<State, Double> prove(StateProofGraph pg) {
		Map<State,Double> ret = super.prove(pg);
		
		//after proving, print top paths for each solution
		System.out.println("Q "+pg.getExample().getQuery().toString());
		for (Map.Entry<State,Double> e : Dictionary.sort(ret)) {
			if (!paths.containsKey(e.getKey())) continue;
			Query q = pg.fill(e.getKey());
			System.out.println("A   "+q.toString()+"    "+e.getValue());
			for (WeightedPath p : paths.get(e.getKey()).result()) {
				System.out.println("P     "+p.humanReadable(pg, ret));
			}
		}
		return ret;
	}
	
	public static void main(String[] args) throws LogicProgramException {
		CustomConfiguration c = new CustomConfiguration(args,
				Configuration.USE_PARAMS, //input
				0, //output
				Configuration.USE_WAM|Configuration.USE_SQUASHFUNCTION, //constants
				0 //modules
				) {
			String query;
			@Override
			protected void addCustomOptions(Options options, int[] flags) {
				options.getOption(Configuration.PARAMS_FILE_OPTION).setRequired(false);
				options.addOption(
						OptionBuilder.withLongOpt("query")
						.withArgName("functor(arg1,Var1)")
						.hasArg()
						.isRequired()
						.withDescription("specify query to print top paths for")
						.create());
				//TODO: add prompt option (for large datasets)
			}
			@Override
			protected void retrieveCustomSettings(CommandLine line,
					int[] flags, Options options) {
				query = line.getOptionValue("query");
			}
			@Override
			public Object getCustomSetting(String name) {
				return query;
			}
		};
		PathDprProver p = new PathDprProver(c.apr);

		Query query = Query.parse((String) c.getCustomSetting(null));
		StateProofGraph pg = new StateProofGraph(query,c.apr,c.program,c.plugins);
		p.prove(pg);
	}
}
