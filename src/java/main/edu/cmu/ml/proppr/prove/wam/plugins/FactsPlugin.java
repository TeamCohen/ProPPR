package edu.cmu.ml.proppr.prove.wam.plugins;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.skjegstad.utils.BloomFilter;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.ParsedFile;

public class FactsPlugin extends WamPlugin {
	private static final Logger log = Logger.getLogger(FactsPlugin.class);
	public static final String FILE_EXTENSION="facts";
	public static final boolean DEFAULT_INDICES=false;
	protected Map<Goal,Double> fd = new HashMap<Goal,Double>();
	protected Map<String,List<String[]>> indexJ = new HashMap<String,List<String[]>>();
	protected Map<JumpArgKey,List<String[]>> indexJA1 = new HashMap<JumpArgKey,List<String[]>>();
	protected Map<JumpArgKey,List<String[]>> indexJA2 = new HashMap<JumpArgKey,List<String[]>>();
	protected Map<JumpArgArgKey,List<String[]>> indexJA1A2 = new HashMap<JumpArgArgKey,List<String[]>>();
	// collected stats on how various indexes are used....
	int numUsesGoalsMatching = 0;
	int numUsesIndexF = 0;
	int numUsesIndexFA1 = 0;
	int numUsesIndexFA2 = 0;
	int numUsesIndexFA1A2 = 0;
	boolean useTernaryIndex;
	private String name;
	public FactsPlugin(APROptions apr, String name, boolean useTernaryIndex) {
		super(apr);
		this.fd.put(WamPlugin.pluginFeature(this, name),1.0);
		this.name = name;
		this.useTernaryIndex = useTernaryIndex;
	}
	
	private static <T> void add(Map<T,List<String[]>> map, T key, String[] args) {
		if (!map.containsKey(key)) map.put(key, new LinkedList<String[]>());
		map.get(key).add(args);
	}
	
	public void addFact(String functor, String ... args) {
		String jump = functor + "/" + args.length;
		add(indexJ, jump, args);
		
		add(indexJA1, new JumpArgKey(jump, args[0]), args);
		
		if (args.length > 1) {
			add(indexJA2, new JumpArgKey(jump, args[1]), args);
			
			if (useTernaryIndex) {
				add(indexJA1A2, new JumpArgArgKey(jump, args[0], args[1]), args);
			}
		}
	}

	@Override
	public String about() {
		return "facts("+name+")";
	}

	@Override
	public boolean claim(String jumpto) {
		return this.indexJ.containsKey(jumpto);
	}

//	@Override
//	public void restartFD(State state, WamInterpreter wamInterp) {
//		throw new RuntimeException("Not yet implemented");
//	}

	@Override
	public List<Outlink> outlinks(State state, WamInterpreter wamInterp,
			boolean computeFeatures) throws LogicProgramException {
		List<Outlink> result = new LinkedList<Outlink>();
		int arity = Integer.parseInt(state.getJumpTo().split("/")[1]);
		String[] argConst = new String[arity];
		for (int i=0; i<arity; i++) argConst[i]=wamInterp.getConstantArg(arity,i+1);
		List<String[]> values = null;
		// fill values according to the query
		if (argConst[0] == null && (argConst.length == 1 || argConst[1] == null)) {
			values = indexJ.get(state.getJumpTo());
		} else if (argConst[0] != null && (argConst.length == 1 || argConst[1] == null)) {
			values = indexJA1.get(new JumpArgKey(state.getJumpTo(), argConst[0]));
		} else if (argConst[0] == null && argConst.length > 1 && argConst[1] != null) {
			values = indexJA2.get(new JumpArgKey(state.getJumpTo(), argConst[1]));
		} else if (argConst.length > 1 && argConst[0] != null && argConst[1] != null) {
			if (useTernaryIndex) {
				values = indexJA1A2.get(new JumpArgArgKey(state.getJumpTo(), argConst[0], argConst[1]));
			} else {
				values = indexJA1.get(new JumpArgKey(state.getJumpTo(), argConst[0]));
				List<String[]> alternate = indexJA2.get(new JumpArgKey(state.getJumpTo(), argConst[1]));
				// treat null lists as empty lists here - wwc
				if (alternate == null) alternate = new java.util.ArrayList<String[]>();
				if (values == null) values = new java.util.ArrayList<String[]>();
				if (values.size() > alternate.size()) values = alternate;
			}
		} else {
			throw new IllegalStateException("Can't happen");
		}
		// then iterate through what you got
		if (values == null) return result;
		for (String[] val : values) {
			if (!check(argConst,val)) continue;
			wamInterp.restoreState(state);
			for (int i=0; i<argConst.length; i++) {
				if (argConst[i] == null) wamInterp.setArg(arity,i+1,val[i]);
			}
			wamInterp.returnp();
			wamInterp.executeWithoutBranching();
			if (computeFeatures) {
				result.add(new Outlink(this.fd, wamInterp.saveState()));
			} else {
				result.add(new Outlink(null, wamInterp.saveState()));
			}
		}
		return result;
	}
	
	private boolean check(String[] args, String[] against) {
		for (int i=0; i<args.length; i++) {
			if (args[i] != null && !(args[i].equals(against[i]))) return false;
		}
		return true;
	}

	public static class JumpArgKey {
		public final String jump;
		public final String arg;
		public JumpArgKey(String jump, String arg) {
			this.jump = jump;
			this.arg = arg;
		}

		public int hashCode() {
			return jump.hashCode() ^ arg.hashCode();
		}
		public boolean equals(Object o) {
			if (! (o instanceof JumpArgKey)) return false;
			JumpArgKey f = (JumpArgKey) o;
			return this.jump.equals(f.jump) && this.arg.equals(f.arg);
		}
	}

	public static class JumpArgArgKey extends JumpArgKey {
		public final String arg2;
		public JumpArgArgKey(String jump, String arg1, String arg2) {
			super(jump, arg1);
			this.arg2 = arg2;
		}

		public int hashCode() {
			return super.hashCode() ^ arg2.hashCode();
		}
		public boolean equals(Object o) {
			return super.equals(o) && ((JumpArgArgKey)o).arg2.equals(this.arg2);
		}
	}

	public void load(File f, int duplicates) {
		ParsedFile parsed = new ParsedFile(f);
		BloomFilter<String> lines = null;
		if (duplicates>0) lines = new BloomFilter(1e-5,duplicates);
		boolean exceeds=false;
		for (String line : parsed) {
			String[] parts =line.split("\t",2);
			if (parts.length != 2) parsed.parseError("expected at least 2 tab-delimited fields");
			if (duplicates>0) {
				if (lines.contains(line)) {
					log.warn("Skipping duplicate fact at "+f.getName()+":"+parsed.getAbsoluteLineNumber()+": "+line);
					continue;
				}
				else lines.add(line);
				if (!exceeds & parsed.getLineNumber() > duplicates) {
					exceeds=true;
					log.warn("Number of facts exceeds "+duplicates+"; duplicate detection may encounter false positives. We should add a command line option to fix this.");
				}
			}
			addFact(parts[0], parts[1].split("\t"));
		}
	}
	public static FactsPlugin load(APROptions apr, File f, boolean ternary) {
		return load(apr,f,ternary,-1);
	}
	public static FactsPlugin load(APROptions apr, File f, boolean ternary, int duplicates) {
		FactsPlugin p = new FactsPlugin(apr, f.getName(), ternary);
		p.load(f,duplicates);
		return p;
		
	}
}
