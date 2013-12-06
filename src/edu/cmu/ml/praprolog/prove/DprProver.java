package edu.cmu.ml.praprolog.prove;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.prove.LogicProgram.LogicProgramOutlink;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.util.Dictionary;

/**
 * prover using depth-first approximate personalized pagerank
 * @author wcohen,krivard
 *
 */
public class DprProver extends Prover {
    private static final Logger log = Logger.getLogger(DprProver.class);
    public static final double EPS_DEFAULT = 0.0001, MINALPH_DEFAULT=0.1;
    private final double epsilon;
    private final double minAlpha;
    private final double stayProbability;
    private final double moveProbability;
    private long start, last;
    private boolean debugTree = false;
    
    private Map<LogicProgramState,int[]> treeId = new HashMap<LogicProgramState,int[]>();
    private Map<LogicProgramState,Integer> nchildren = new HashMap<LogicProgramState,Integer>();
    public void clearDebugTree() {
    	treeId = new HashMap<LogicProgramState,int[]>();
    	nchildren = new HashMap<LogicProgramState,Integer>();
    }
    private void addToDebugTree(LogicProgramState parent, LogicProgramState child, boolean reset) {
    	if (treeId.containsKey(child) && reset) return;
    	addToDebugTree(parent,child);
    }
    private void addToDebugTree(LogicProgramState parent, LogicProgramState child) {
    	int[] par = treeId.get(parent);
    	int[] ch;
    	if (par==null) { ch = new int[1]; ch[0]=0; }
    	else {

        	if (treeId.containsKey(child) && !child.isSolution()) {
        		int[] pch = treeId.get(child);
        		for (int i=0; i<par.length; i++) {
        			if (i >= pch.length || pch[i] != par[i]) {
        				throw new IllegalStateException("State "+getTreeId(child)+" already added. New parent: "+getTreeId(parent));
        			}
        		}
        		return;
        	}
        	
    		ch = Arrays.copyOf(par, par.length+1);
    		ch[par.length] = nchildren.get(parent)+1;
    		nchildren.put(parent,ch[par.length]);
    	}
    	nchildren.put(child,0);
    	
    	treeId.put(child,ch);
    }
    private String getTreeId(LogicProgramState st) {
    	int[] id = treeId.get(st);
    	if (id==null) throw new IllegalStateException("Never heard of "+st);
    	StringBuilder sb = new StringBuilder();
    	for (int i : id) sb.append(",").append(i);
    	return sb.append("-").append(st.toString()).substring(1);
    }

    public DprProver() { this(false); }
    public DprProver(boolean lazyWalk) {
        this(lazyWalk,EPS_DEFAULT,MINALPH_DEFAULT);
    }
    public DprProver(double epsilon, double minalpha) {
        this(false, epsilon, minalpha);
    }
    public DprProver(boolean lazyWalk, double epsilon, double minalpha) {
        this.epsilon = epsilon;
        this.minAlpha = minalpha;
        if (lazyWalk) this.stayProbability = 0.5;
        else this.stayProbability = 0.0;
        moveProbability = 1.0-stayProbability;
    }
    @Override
    public Map<LogicProgramState, Double> proveState(LogicProgram lp, LogicProgramState state0, GraphWriter gw) {
    	
        Map<LogicProgramState,Double> p = new HashMap<LogicProgramState,Double>();
        Map<LogicProgramState,Double> r = new HashMap<LogicProgramState,Double>();
        if(log.isDebugEnabled()) {
        	debugTree = true;
        	clearDebugTree();
        	addToDebugTree(null, state0);
        	log.debug("r = 1.0 "+getTreeId(state0));
        }
        r.put(state0, 1.0);
        Map<LogicProgramState,Integer> deg = new HashMap<LogicProgramState,Integer>();
        boolean trueLoop=true, restart=false;
        int d = lp.lpDegree(state0,trueLoop,restart);
        deg.put(state0,d);
        if(debugTree) log.debug(String.format("deg = %d %s",d,getTreeId(state0)));
        int numPushes = 0;
        int numIterations = 0;
        for(int pushCounter = 0; ;) {
        	if(debugTree) log.debug("new dfsPushes proveState "+getTreeId(state0));
            start = last = System.currentTimeMillis();
            pushCounter = this.dfsPushes(lp,p,r,deg,state0,gw,0);
            numIterations++;
            if(log.isInfoEnabled()) log.info("Iteration: "+numIterations+" pushes: "+pushCounter+" r-states: "+r.size());
            if(pushCounter==0) break;
            numPushes+=pushCounter;
        }
        if(log.isInfoEnabled()) log.info("total iterations "+numIterations+" total pushes "+numPushes);
        if(debugTree) log.debug("ans:"+Dictionary.buildString(p, new StringBuilder(), "\n").toString());
        return p;
    }

    private int dfsPushes(LogicProgram lp, Map<LogicProgramState,Double> p, Map<LogicProgramState, Double> r,
            Map<LogicProgramState, Integer> deg, LogicProgramState u, GraphWriter gw, int pushCounter) {

//        if (log.isDebugEnabled()) {
//        	double pvalues = 0.0, rvalues = 0.0;
//        	for (Double d : p.values()) pvalues += d;
//        	for (Double d : r.values()) rvalues += d;
//        	StringBuilder sb = new StringBuilder();
//        	for (int d=0; d<u.getDepth(); d++) sb.append("| ");
//        	log.debug(String.format("sum p %f sum r %.8f del %.8f",pvalues,rvalues,r.get(u) / deg.get(u)));
//        	log.debug(String.format("%s%s ru %.8f degu %d",sb.toString(),u.toString(),r.get(u),deg.get(u)));
//        }
        if (r.get(u) / deg.get(u) > epsilon) {
        	if (debugTree) log.debug("push "+pushCounter+"->"+(pushCounter+1)+" "+getTreeId(u));
        	else if (log.isInfoEnabled()) {
        		long now = System.currentTimeMillis(); 
        		if (now - last > 1000) {
        			log.info("push "+pushCounter+"->"+(pushCounter+1)+" "+r.size()+" r-states u "+u);
        			last = now;
        		}
//        		if (now - start > 10000) {
//        			log.setLevel(Level.DEBUG);
//        		}
        	}
            pushCounter += 1;
            double ru = r.get(u);
            LogicProgramOutlink restart = lp.lpRestartWeight(u,true); // trueLoop
            if (debugTree) {
            	addToDebugTree(u, restart.state, true);
            	log.debug("outlink "+restart.weight+" "+Dictionary.buildString(restart.featureList, new StringBuilder(), ",")+" "+getTreeId(restart.state));
            } else if (log.isDebugEnabled()) log.debug("restart weight for pushlevel "+pushCounter);
            double unNormalizedAlpha = restart.getWeight();
            
            List<LogicProgramOutlink> outs = lp.lpOutlinks(u,true,false); // trueloop, restart
            if (!debugTree && log.isDebugEnabled()) log.debug("outlinks for pushlevel "+pushCounter+": "+outs.size());
            double z= unNormalizedAlpha; 
            double m=0.0;
            for (LogicProgramOutlink o : outs) {
            	if (debugTree) {
            		addToDebugTree(u,o.state);
            		log.debug("outlink "+o.weight+" "+Dictionary.buildString(o.featureList, new StringBuilder(), ",")+" "+getTreeId(o.state));
            	}
            	z += o.getWeight();
            	m = Math.max(m,o.getWeight());
            }

            double localAlpha = unNormalizedAlpha / z;
            
            if (debugTree) {
            	String id= getTreeId(u);
            	log.debug("localAlpha = "+localAlpha+" "+id);
            	log.debug(String.format("p = %.8f %s ",p.get(u),id));
            }
            if (localAlpha < this.minAlpha) {
            	log.warn("max outlink weight="+m+"; numouts="+outs.size()+"; unAlpha="+restart.getWeight()+"; z="+z);
                throw new MinAlphaException(minAlpha,localAlpha,u);
            }
            Dictionary.increment(p,u,minAlpha * ru,"(elided)");
            r.put(u, r.get(u) * stayProbability * (1.0-minAlpha));
            if (debugTree) {
            	String id= getTreeId(u);
            	log.debug(String.format("p += (minalpha)(ru) = (%.8f)(%.8f) = %.8f %s ",this.minAlpha,ru,this.minAlpha*ru,id));
            	log.debug(String.format("p = %.8f %s ",p.get(u),id));
            	log.debug(String.format("r = %.8f %s ",r.get(u),id));
            }
//            if (log.isDebugEnabled()) {
//            	log.debug("ru before reset: "+r.get(u));
//            	log.debug("reset weight: "+(z * (localAlpha - minAlpha)));
//            }
            
//            if(log.isDebugEnabled()) log.debug("including "+outs.size()+" outlinks");
            for (LogicProgramOutlink o : outs) {
            	if (debugTree) log.debug("include "+getTreeId(o.state));
                includeState(o,r,deg,z,ru,lp);
            }
            // include the reset state with weight (alph - minAlpha):
            restart.weight = z * (localAlpha - minAlpha);
        	if (debugTree) log.debug("include "+getTreeId(restart.state));
            includeState(restart,r,deg,z,ru,lp);

            if (gw!=null) gw.writeEdge(u, u.restart(), restart.getFeatureList());
            for (LogicProgramOutlink o : outs) {
            	if (gw != null) gw.writeEdge(u, o.getState(), o.getFeatureList());
            	// current pushcounter is passed down, gets incremented and returned, and 
            	// on the next for loop iter is passed down again...
            	if (debugTree) {
            		log.debug("new dfsPushes dfsPushes "+getTreeId(o.state));
            	}
                pushCounter = this.dfsPushes(lp,p,r,deg,o.getState(),gw,pushCounter);
            }
        }
        if (debugTree) log.debug("close dfsPushes "+pushCounter+" "+getTreeId(u));
        return pushCounter;
    }
    private void includeState(LogicProgramOutlink o, Map<LogicProgramState, Double> r,
            Map<LogicProgramState, Integer> deg, double z, double ru, LogicProgram lp) {
        boolean followup = !r.containsKey(o.getState());
        double old = Dictionary.safeGet(r, o.getState());
//        if (log.isDebugEnabled()) {
//        	log.debug("    was: "+r.get(o.getState())+" on "+o.getState());
//        }
        Dictionary.increment(r, o.getState(), moveProbability * (o.getWeight() / z) * ru,"(elided)");
        if(followup) {
        	if (debugTree) log.debug("deg = "+lp.lpDegree(o.getState(),true,true)+" "+getTreeId(o.state));
        	deg.put(o.getState(),lp.lpDegree(o.getState(),true,true)); // trueloop, restart
        }
        if (debugTree) {
//        	log.trace(String.format("+=reset: %.8f from %.8f on %s",r.get(o.getState()),old,o.getState()));
        	log.debug(String.format("r += wt*ru/z = %.8f*%.8f/%.8f = %.8f %s ",o.getWeight(),ru,z,o.getWeight()*ru/z,getTreeId(o.state)));
        	log.debug(String.format("r = %.8f %s ",r.get(o.getState()),getTreeId(o.state)));
            }
        if (deg.get(o.getState()) == 0)
            throw new IllegalStateException("Zero degree for "+o.getState());
    }
	public double getAlpha() {
		return this.minAlpha;
	}

}
