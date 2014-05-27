package edu.cmu.ml.praprolog.prove;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.unsafe.batchinsert.BatchInserter;

import edu.cmu.ml.praprolog.util.ParsedFile;

public class Neo4jGraphComponent extends GraphlikeComponent {
	private static final Map<String, Object> NO_PROPERTIES = Collections.emptyMap();
	private static final Logger log = Logger.getLogger(Neo4jGraphComponent.class);
	private static final String NODENAME_KEY = "name";
	private static final String FUNCTOR_KEY = "name";
	private static final String NODEINDEX = "nodes";
	private static final String RELINDEX = "relationships";
	private static final int DEFAULT_CACHE = 100000;
	private static final int DEFAULT_BATCH = 1000;
	private static final String NODEID_KEY = "id";

	private static GraphDatabaseService graphDb;
	private static Index<Node> nodeIndex;
	private static RelationshipIndex relIndex;

	private static void init(String dbPath) {
		if (graphDb != null) return;
		if (new File("neo4j.properties").exists())
			graphDb = new GraphDatabaseFactory()
		.newEmbeddedDatabaseBuilder( dbPath )
		.loadPropertiesFromFile("neo4j.properties")
		.newGraphDatabase();
		else {
			Map<String,String> config = new TreeMap<String,String>();
			config.put("keep_logical_logs","false");
			graphDb = new GraphDatabaseFactory()
			.newEmbeddedDatabaseBuilder( dbPath )
			.setConfig(config)
			.newGraphDatabase();	
		}
		nodeIndex = graphDb.index().forNodes( NODEINDEX );
		relIndex = graphDb.index().forRelationships( RELINDEX );
		registerShutdownHook( graphDb );
	}

	protected Map<Goal, Double> featureDict;

	public Neo4jGraphComponent(String dbPath) {
		init(dbPath);
		this.featureDict = new HashMap<Goal,Double>();
		this.featureDict.put(new Goal("id",dbPath),1.0);
	}

	@Override
	protected void _indexAppend(String functor, Argument src, Argument dst) {
		Transaction tx = graphDb.beginTx();
		try
		{
			this._tx_indexAppend(functor, src, dst);
			tx.success();
		}
		finally
		{
			tx.finish();
		}
	}
	private void _tx_indexAppend(String functor, Argument src, Argument dst) {
		Node srcNode = getOrMakeNode(src.getName());
		Node dstNode = getOrMakeNode(dst.getName());

		Relationship relationship = srcNode.createRelationshipTo( dstNode, DynamicRelationshipType.withName(functor) );
		relationship.setProperty( FUNCTOR_KEY, functor );
		relIndex.add( relationship, FUNCTOR_KEY, functor);
	}
	private Node getOrMakeNode(String name) {
		IndexHits<Node> hits = nodeIndex.get(NODENAME_KEY, name);
		if (hits.hasNext()) {
			return hits.getSingle();
		} else {
			Node ret = graphDb.createNode();
			ret.setProperty( NODENAME_KEY, name );
			nodeIndex.add( ret, NODENAME_KEY, name );
			return ret;
		}
	}

	@Override
	protected boolean _indexContains(String functor) {
		return relIndex.get(FUNCTOR_KEY, functor).hasNext();
	}

	@Override
	protected List<Argument> _indexGet(String functor, Argument srcConst) {
		IndexHits<Node> hits = nodeIndex.get( NODENAME_KEY, srcConst.getName() );
		ArrayList<Argument> ret = new ArrayList<Argument>();
		for (Node srcNode : hits) {
			for (Relationship r : srcNode.getRelationships(DynamicRelationshipType.withName(functor), Direction.OUTGOING)) {
				ret.add(new ConstantArgument((String) r.getEndNode().getProperty(NODENAME_KEY)));
			}
		}
		return ret;
	}

	@Override
	protected int _indexGetDegree(String functor, Argument srcConst) {
		IndexHits<Node> hits = nodeIndex.get( NODENAME_KEY, srcConst.getName() );
		int count=0;
		for (Node srcNode : hits) {
			for (Relationship r : srcNode.getRelationships(DynamicRelationshipType.withName(functor), Direction.OUTGOING)) { 
				count++;
			}
		}
		return count;
	}

	@Override
	protected Map<Goal, Double> getFeatureDict() {
		return this.featureDict;
	}

	private static void usage() {

		System.err.println("Usage:\n\tload-nodes dbPath file1.i file2.i ... \n"+
				"\tload-edges dbPath nodeCacheSize file1.cfacts file2.cfacts ... \n"+
				"\tload-tx dbPath graphFile1 graphFile2 ...\n"+
				"\tquery dbPath");
		System.exit(0);
	}
	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
		}

		String cmd=args[0], dbPath = args[1]; 
		if ("load-tx".equals(cmd)) {
			if (args.length < 3) usage();

			long t0 = System.currentTimeMillis();
			Neo4jGraphComponent nc = new Neo4jGraphComponent(dbPath);
			long t1 = System.currentTimeMillis();
			System.out.println("open "+dbPath+" "+(t1-t0));
			t1 = t0 = System.currentTimeMillis();
			for (int i=2; i<args.length; i++) {
				String graphFile = args[i];
				Neo4jGraphComponent.load(nc, graphFile);
				long t2 = System.currentTimeMillis();
				System.out.println("load "+graphFile+" "+(t2-t1));
				t1 = t2;
			}
			t1 = System.currentTimeMillis();
			System.out.println("load all "+(t1-t0));

		} else if ("load-nodes".equals(cmd)) {
			if (args.length < 3) usage();
			loadBatchNodes(dbPath,Arrays.copyOfRange(args, 2, args.length));
		} else if ("load-edges".equals(cmd)) {
			if (args.length < 4) usage();
			loadBatchRelationships(dbPath, Integer.parseInt(args[2]), Arrays.copyOfRange(args,4,args.length));
		} else if ("query".equals(cmd)) {
			long t0 = System.currentTimeMillis();
			Neo4jGraphComponent nc = new Neo4jGraphComponent(dbPath);
			long t1 = System.currentTimeMillis();
			System.out.println("open "+dbPath+" "+(t1-t0));

			while( !"quit".equals(cmd=System.console().readLine(" > "))) {
				String[] parts = cmd.split(" ");
				String functor = parts[0],arg1=parts[1];
				t1 = System.currentTimeMillis();
				List<Argument> result = nc._indexGet(functor, new ConstantArgument(arg1));
				long t2 = System.currentTimeMillis();
				System.out.println(result.size()+" results ("+(t2-t1)+")");
				if (parts.length > 2 && "print".equals(parts[2])) {
					t1 = System.currentTimeMillis();
					for (Argument a : result) {
						t2 = System.currentTimeMillis();
						System.out.println("\t"+a+" ("+(t2-t1)+")");
						t1 = System.currentTimeMillis();
					}
				}
			}
		} else {
			System.err.println("No command '"+cmd+"'");
			usage();
		}

	}

	public static void loadBatchRelationships(String dbDir, int cache,  String ... files) {
		if (files.length == 0) return;

		BatchInserter inserter = BatchInserters.inserter(dbDir);

		BatchInserterIndexProvider indexProvider =
				new LuceneBatchInserterIndexProvider( inserter );
		BatchInserterIndex nodes =
				indexProvider.nodeIndex( NODEINDEX, MapUtil.stringMap( "type", "exact" ) );
		nodes.setCacheCapacity( NODENAME_KEY, cache );

		BatchInserterIndex relationships = 
				indexProvider.relationshipIndex( RELINDEX, MapUtil.stringMap( "type", "exact" ) );

		// then load a pile of relationships
		ParsedFile reader=null;

		long[] from = new long[DEFAULT_BATCH],
				to = new long[DEFAULT_BATCH];
		String[] functor = new String[DEFAULT_BATCH];
		int k=0;
		String xTypeQuery = NODEID_KEY+":\"";
		String xNameQuery = " "+NODENAME_KEY+":\"";
		for (String file : files) {
			if (!file.endsWith(GoalComponent.FILE_EXTENSION)) {
				log.error("Skipping file "+file+" (needs "+GoalComponent.FILE_EXTENSION+")");
				continue;
			} else 
				log.info("Reading file "+file);
			String[] types = baseName( file, GoalComponent.FILE_EXTENSION ).split("_");
			if (types.length != 3) {
				log.error("Skipping file "+file+" (needs name format functor_arg1type_arg2type)");
				continue;
			}
			//			String fromTypeQuery = xTypeQuery+types[1]+"\" AND"+xNameQuery;
			//			String toTypeQuery = xTypeQuery+types[2]+"\" AND"+xNameQuery;
			reader = new ParsedFile(file);
			long last = System.currentTimeMillis(), now = last, first = last;
			log.info("Read "+reader.getLineNumber()+" lines "+now+" 0 lps");
			log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

			for (String line : reader) {
				if (k>=from.length) k=commit(inserter,relationships,from,to,functor,k,reader.getLineNumber());

				if( (now = System.currentTimeMillis()) - last > 2000)  {
					log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
					last = now;
					log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
				}
				String[] parts = line.split("\t");
				if (parts.length < 3) {
					log.error("Bad format of "+file+" line "+reader.getLineNumber()+" (needs functor/targ1/targ2):" + line);
					reader.close();
					continue;
				}

				functor[k] = parts[0];
				from[k] = nodes.get(NODEID_KEY, types[1]+parts[1]).getSingle();//queryHelper(fromTypeQuery,parts[1],nodes,line);
				to[k] = nodes.get(NODEID_KEY, types[2]+parts[2]).getSingle();//queryHelper(toTypeQuery,parts[2],nodes,line);
				k++;
			}
			k=commit(inserter,relationships,from,to,functor,k,reader.getLineNumber());
			now = System.currentTimeMillis();
			log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
			log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

			reader.close();
		}

		relationships.flush();
		inserter.shutdown();
		indexProvider.shutdown();
	}
	private static long queryHelper(String baseQuery, String term, BatchInserterIndex nodes,String line) {
		String query = baseQuery+term+"\"";
		IndexHits<Long> results = nodes.query(query);
		if (results.size() != 1) {
			log.error("Problem adding relationship "+line);
			log.error(results.size()+" nodes for query "+query);
			if (results.size() < 1000) {
				for (long l : results) {
					log.error(l);
				}
			} 
		}
		return results.getSingle();
	}
	private static int commit(BatchInserter inserter, BatchInserterIndex relationships, long[] from, long[] to, String[] functor, int k, int lineNumber) {
		for (int i=0; i<k; i++) {
			try {
				long rel = inserter.createRelationship( from[i], to[i], DynamicRelationshipType.withName(functor[i]), null );
				relationships.add(rel, NO_PROPERTIES);
			} catch (Exception e) {
				throw new RuntimeException("Couldn't add relationship for line "+(lineNumber - k + i)+"; ("+from[i]+" -> "+to[i]+")",e);
			}
		}
		return 0;
	}
	private static String baseName(String path, String suffix) {
		String base = path.substring(path.lastIndexOf(File.separatorChar)+1);
		base = base.substring(0,base.lastIndexOf(suffix));
		return base;
	}
	public static void loadBatchNodes(String dbDir, String ... files) {
		if (files.length == 0) return;

		BatchInserter inserter = BatchInserters.inserter(dbDir);
		Map<String, Object> properties = new HashMap<String, Object>();

		BatchInserterIndexProvider indexProvider =
				new LuceneBatchInserterIndexProvider( inserter );
		BatchInserterIndex nodes =
				indexProvider.nodeIndex( NODEINDEX, MapUtil.stringMap( "type", "exact" ) );
		nodes.setCacheCapacity( NODENAME_KEY, DEFAULT_CACHE );

		// then load a pile of nodes
		ParsedFile reader=null;
		int numnodes=0;
		for (String file : files) {
			if (!file.endsWith(SparseGraphComponent.INDEX_EXTENSION)) {
				log.error("Skipping file "+file+" (needs "+SparseGraphComponent.INDEX_EXTENSION+")");
				continue;
			} else 
				log.info("Reading file "+file);
			String type = baseName(file,SparseGraphComponent.INDEX_EXTENSION);
			//			properties.put( NODEID_KEY, baseName(file,SparseGraphComponent.INDEX_EXTENSION) );
			reader = new ParsedFile(file);
			long last = System.currentTimeMillis(), now = last, first = last;
			log.info("Read "+reader.getLineNumber()+" lines "+now+" 0 lps");
			log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

			for (String line : reader) {
				if( (now = System.currentTimeMillis()) - last > 2000)  {
					log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
					last = now;
					log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
				}

				properties.put( NODENAME_KEY, line );
				properties.put( NODEID_KEY, type+line);
				long node = inserter.createNode( properties );
				nodes.add(node, properties);
				numnodes++;

			}
			now = System.currentTimeMillis();
			log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
			log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
			reader.close();
		}
		log.info("Loaded "+numnodes+" nodes in total");
		nodes.flush();
		inserter.shutdown();
		indexProvider.shutdown();
	}

	/**
	 * Load a Neo4jGraphComponent with data from
        a file.  The format of the file is that each line is a tab-separated 
        triple of edgelabel, sourceNode, destNode.
	 * @param result
	 * @param graphFile
	 * @return
	 */
	public static Neo4jGraphComponent load(Neo4jGraphComponent result, String graphFile) {
		Transaction tx = graphDb.beginTx();
		ParsedFile reader=null;
		try {
			reader = new ParsedFile(graphFile);

			long last = System.currentTimeMillis(), now = last, first = last;
			log.info("Read "+reader.getLineNumber()+" lines "+now+" 0 lps");
			log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
			for(String line : reader) {
				if(reader.getLineNumber() % 20000 == 0) { tx.success(); tx.finish(); tx = graphDb.beginTx(); }
				if( (now = System.currentTimeMillis()) - last > 2000)  {
					log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
					last = now;
					log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
				}
				String[] parts = line.split("\t");
				if (parts.length != 3) reader.parseError("Bad line (must be $edge\t$src\t$dst)");
				String edgeLabel = parts[0].trim();
				String src = parts[1].trim();
				String dst = parts[2].trim();
				result._tx_indexAppend(edgeLabel, Argument.fromString(src), Argument.fromString(dst));
			}
			now = System.currentTimeMillis();
			log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
			log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

			reader.close();
			tx.success();
		} catch (IllegalArgumentException e) {
			throw(e);
		}
		finally
		{
			tx.finish();
			if (reader!=null) reader.close();
		}
		return result;
	}


	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				graphDb.shutdown();
			}
		} );
	}
}
