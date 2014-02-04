package edu.cmu.ml.praprolog.prove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.prove.Component.Outlink;
import edu.cmu.ml.praprolog.util.SymbolTable;


public class LogicProgram {
        private static final Logger log = Logger.getLogger(LogicProgram.class);
		public static final boolean DEFAULT_TRUELOOP = true;
		public static final boolean DEFAULT_RESTART = false;
	protected Component[] components;
	protected FeatureDictWeighter weighter = new UniformWeighter();
	protected Map<Goal,Double> trueLoopFeatureDict = new HashMap<Goal,Double>();
	protected Map<Goal,Double> trueLoopRestartFeatureDict = new HashMap<Goal,Double>();
//	protected GraphWriter writer=null;
	protected SymbolTable symbolTable;
//	public LogicProgram(String fileNameList) {
//		this(Component.loadComponents(fileNameList.split(":")));
//	}
	public LogicProgram(Component ... cmpnts) {
		this.components = Arrays.copyOf(cmpnts, cmpnts.length);
		symbolTable = new SymbolTable();
		for (Component c : this.components) {
			if (c==null) continue;
//			log.debug("Compiling "+c);
			c.compile(symbolTable);
//			log.debug("Compiled "+c);
		}
		trueLoopFeatureDict.put(new Goal("id","trueLoop"), 1.0);
		trueLoopRestartFeatureDict.put(new Goal("id","trueLoopRestart"), 1.0);
	}
	public LogicProgram(LogicProgram p) {
		this.components=p.components;
		this.symbolTable = p.symbolTable;
		this.trueLoopFeatureDict.putAll(p.trueLoopFeatureDict);
		this.trueLoopRestartFeatureDict.putAll(p.trueLoopRestartFeatureDict);
	}
	public void setFeatureDictWeighter(FeatureDictWeighter w) {
		this.weighter = w;
	}
	public Iterable<LogicProgramOutlink> lpNormalizedOutlinks(LogicProgramState state, boolean trueloop,
			boolean restart) throws LogicProgramException {
		List<LogicProgramOutlink> outList = this.lpOutlinks(state, trueloop, restart);
		double z = 0;
		for (LogicProgramOutlink o : outList) z += o.getWeight();
		List<LogicProgramOutlink> result = new ArrayList<LogicProgramOutlink>();
		for (LogicProgramOutlink o : outList) result.add(new LogicProgramOutlink(o.getWeight()/z, o.getState(), o.getFeatureList()));
		return result;
	}
	/**
	 * Find out which component claims this goal, and use it to generate 
     *  outlinks, but replace the feature dictionary with a computed weight.
     *  If trueLoop is set then include self-loops from a solution to itself.
     *  If restart is set then include explicit edges to a restart state.
	 * @param state
	 * @param trueloop
	 * @param restart
	 * @return
	 * @throws LogicProgramException 
	 */
	public List<LogicProgramOutlink> lpOutlinks(LogicProgramState state, boolean trueloop,
			boolean restart) throws LogicProgramException {
		List<LogicProgramOutlink> result = new ArrayList<LogicProgramOutlink>();
		if (state.isSolution()) {
			if (trueloop) {
				result.add(weightEdge(this.trueLoopFeatureDict,state,state));
			} if (restart) {
				result.add(weightEdge(this.trueLoopRestartFeatureDict,state,state.restart()));
			}
			return result;
		} else {
			for (Component c : this.components) {
				if (c.claim(state)) {
					if (log.isInfoEnabled()) log.info(state+"\n\tclaimed by "+c);
					for (Outlink o : c.outlinks(state)) {
						result.add(this.weightEdge(o.getFeatureDict(),state,o.getState()));
					}
					if (restart) {
						result.add(this.weightEdge(c.restartFeatureDict(state),state,state.restart()));
					}
					return result;
				}
			}
			throw new LogicProgramException("No definition for "+state.getHeadFunctor()+"/"+state.getHeadArity()+"("+state.getHeadArg1()+" ...)");
		}
	}

	/**
	 * Convert the featureDict to a numeric weight, and possibly
        add an edge to the graph with the writer.  To make sure the
        graph that is traversed is written, every edge yielded by
        lpOutlinks should be yielded by a call
        yield(self._weightEdge(fd,state,child))
	 * @param featureDict
	 * @param state
	 * @param child
	 * @return
	 */
	protected LogicProgramOutlink weightEdge(Map<Goal, Double> featureDict,
			LogicProgramState state, LogicProgramState child) {
//		if (this.writer != null) {
//			this.writer.writeEdge(state,child,Feature.toFeatureList(featureDict));
//		}
		if (log.isDebugEnabled()) log.debug("weightEdge "+child);
		return new LogicProgramOutlink(this.weighter.w(featureDict), child, Feature.toFeatureList(featureDict));
	}

	public static class LogicProgramOutlink {
		double weight;
		LogicProgramState state;
		List<Feature> featureList;
		public LogicProgramOutlink(double w, LogicProgramState s, List<Feature> f) {
			this.weight = w;
			this.state = s;
			this.featureList = f;
		}
		public double getWeight() {
			return weight;
		}
		public LogicProgramState getState() {
			return state;
		}
		public List<Feature> getFeatureList() {
			return featureList;
		}
	}

	/**
	 * 
	 * @param state
	 * @param trueLoop
	 * @param restart
	 * @return
	 * @throws LogicProgramException 
	 */
    public int lpDegree(LogicProgramState state, boolean trueLoop, boolean restart) throws LogicProgramException {
    	if(log.isDebugEnabled()) log.debug("degree of "+state);
        if (state.isSolution()) {
            int d=0;
            if (trueLoop) d++;
            if (restart) d++;
            return d;
        } else {
            for (Component c : this.components) {
                if (c.claim(state)) {
                	if (log.isDebugEnabled()) log.debug("Claimed by "+c.getClass().getCanonicalName());
                    int d = c.degree(state);
                    if (restart) d++;
                    return d;
                }
            }
        }
        throw new LogicProgramException("No definition for "+state.getHeadFunctor()+"/"+state.getHeadArity()+"("+state.getHeadArg1()+" ...)");
    }
    /**
     * The weight that would be assigned to the restart from this
        state.  If writer is present, also writes an appropriate
        restart edge in the graph.
     * @param state
     * @param trueLoop
     * @return
     * @throws LogicProgramException 
     */
    public LogicProgramOutlink lpRestartWeight(LogicProgramState state, boolean trueLoop) throws LogicProgramException {
        if (state.isSolution() && trueLoop) {
            return weightForRestartEdge(trueLoopRestartFeatureDict,state);
        } else {
            for (Component c : this.components) {
                if (c.claim(state)) {
                    return weightForRestartEdge(c.restartFeatureDict(state), state);
                }
            }
        }
        throw new LogicProgramException("No definition for "+state.getHeadFunctor()+"/"+state.getHeadArity()+"("+state.getHeadArg1()+" ...)");
    }
    /**
     * Convert the featureDict to a numeric weight, and possibly
        add an edge to the graph with the writer.  To make sure the
        graph that is traversed is written, every edge yielded by
        lpOutlinks should be yielded by a call
        yield(self._weightEdge(fd,state,child))
     * @param fd
     * @param state
     * @return
     */
    protected LogicProgramOutlink weightForRestartEdge(Map<Goal, Double> featureDict, LogicProgramState state) {
        return new LogicProgramOutlink(this.weighter.w(featureDict), state.restart(), Feature.toFeatureList(featureDict));
    }
	public SymbolTable getSymbolTable() {
		return this.symbolTable;
	}
	
	public String listing() {
		StringBuilder sb = new StringBuilder("logicProgram:");
		for (Component c : this.components) sb.append("\n").append(c.listing());
		sb.append("\n").append(this.weighter.listing());
		return sb.toString();
	}
	public void setAlpha(double d) {
		for (Component c : this.components) c.setAlpha(d);
	}
}
