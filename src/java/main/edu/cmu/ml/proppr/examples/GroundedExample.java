package edu.cmu.ml.proppr.examples;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.graph.InferenceGraph;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.Dictionary;

public class GroundedExample {
		protected List<State> posList;
		protected List<State> negList;
		protected Map<State,Double> queryVec;
		protected InferenceGraph graph;

		private GroundedExample(InferenceGraph graph, Map<State,Double> queryVec) {
			this.queryVec = queryVec;
			this.graph = graph;
		}
		
		public GroundedExample(InferenceGraph graph, Map<State,Double> queryVec,
				List<State> pos, List<State> neg) {
			this(graph,queryVec);
			this.posList = pos;
			this.negList = neg;
		}

		public int length() {
			return posList.size() + negList.size();
		}

		public String toString() {
			final StringBuilder sb = new StringBuilder("PosNegRWExample[");
			sb.append(graph.nodeSize()).append("/").append(graph.edgeSize()).append("; [");
			for (State q : this.queryVec.keySet()) {
				sb.append("'").append(q).append("',");
			}
			sb.deleteCharAt(sb.length());
			sb.append("] -> +[");
			if (posList.size() > 0) { sb.append("'"); Dictionary.buildString(posList, sb, "','"); sb.append("'"); };
			sb.append("]; -[");
			if (negList.size() > 0) { sb.append("'"); Dictionary.buildString(negList, sb, "','"); sb.append("'"); };
			sb.append("]]");
			return sb.toString();
		}

		public List<State> getPosList() {
			return posList;
		}

		public List<State> getNegList() {
			return negList;
		}

		public Map<State, Double> getQueryVec() {
			return this.queryVec;
		}

		public InferenceGraph getGraph() {
			return this.graph;
		}
}
