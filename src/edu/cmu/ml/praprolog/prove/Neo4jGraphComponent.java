package edu.cmu.ml.praprolog.prove;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.neo4j.graphdb.RelationshipType;
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

public class Neo4jGraphComponent extends GraphlikeComponent {
	private static final Logger log = Logger.getLogger(Neo4jGraphComponent.class);
	//	private static enum RelTypes implements RelationshipType {
	//	    FUNCTOR
	//	}
	private static final String NODENAME_KEY = "name";
	private static final String FUNCTOR_KEY = "name";
	private static final int DEFAULT_CACHE = 100000;
	private static final String NODEINDEX = "nodes";
	private static final String RELINDEX = "relationships";

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
		Node srcNode = nodeIndex.get( NODENAME_KEY, srcConst.getName() ).getSingle();
		ArrayList<Argument> ret = new ArrayList<Argument>();
		for (Relationship r : srcNode.getRelationships(DynamicRelationshipType.withName(functor), Direction.OUTGOING)) {
			ret.add(new ConstantArgument((String) r.getEndNode().getProperty(NODENAME_KEY)));
		}
		return ret;
	}

	@Override
	protected int _indexGetDegree(String functor, Argument srcConst) {
		Node srcNode = nodeIndex.get( NODENAME_KEY, srcConst.getName() ).getSingle();
		int count=0;
		for (Relationship r : srcNode.getRelationships(DynamicRelationshipType.withName(functor), Direction.OUTGOING)) count++;
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
			loadBatchRelationships(dbPath, Integer.parseInt(args[2]), Arrays.copyOfRange(args,3,args.length));
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

	public static void loadBatchRelationships(String dbDir, int cache, String ... files) {
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
		LineNumberReader reader=null;
		for (String file : files) {
			if (!file.endsWith(GoalComponent.FILE_EXTENSION)) {
				log.error("Skipping file "+file+" (needs "+GoalComponent.FILE_EXTENSION+")");
				continue;
			}
			try {
				reader = new LineNumberReader(new FileReader(file));
				long last = System.currentTimeMillis(), now = last, first = last;
				log.info("Read "+reader.getLineNumber()+" lines "+now+" 0 lps");
				log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
				
				for (String line; (line=reader.readLine())!= null;) {
					if( (now = System.currentTimeMillis()) - last > 2000)  {
						log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
						last = now;
						log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
					}
					line = line.trim();
					String[] parts = line.split("\t");
					if (parts.length < 3) {
						log.error("Bad format of "+file+" line "+reader.getLineNumber()+" (needs functor/targ1/targ2):" + line);
						continue;
					}

					long fromnode = nodes.get(NODENAME_KEY, parts[1]).getSingle();
					long tonode = nodes.get(NODENAME_KEY, parts[2]).getSingle();
					long rel = inserter.createRelationship( fromnode, tonode, DynamicRelationshipType.withName(parts[0]), null );
					relationships.add(rel, null);

				}
				now = System.currentTimeMillis();
				log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
				log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

			} catch (FileNotFoundException e) { log.error("Couldn't find file "+file+";",e);
			} catch (IOException e) { log.error("Trouble reading file "+file+";",e);
			} finally {
				if (reader!=null)
					try {
						reader.close();
					} catch (IOException e) { log.error("Trouble closing file "+file+";",e); }
			}
		}

		relationships.flush();
		inserter.shutdown();
		indexProvider.shutdown();
	}
	public static void loadBatchNodes(String dbDir, String ... files) {
		if (files.length == 0) return;

		BatchInserter inserter = BatchInserters.inserter(dbDir);
		Map<String, Object> properties = new HashMap<String, Object>();

		BatchInserterIndexProvider indexProvider =
				new LuceneBatchInserterIndexProvider( inserter );
		BatchInserterIndex nodes =
				indexProvider.nodeIndex( "nodes", MapUtil.stringMap( "type", "exact" ) );
		nodes.setCacheCapacity( NODENAME_KEY, DEFAULT_CACHE );

		// then load a pile of nodes
		LineNumberReader reader=null;
		for (String file : files) {
			if (!file.endsWith(SparseGraphComponent.INDEX_EXTENSION)) {
				log.error("Skipping file "+file+" (needs "+SparseGraphComponent.INDEX_EXTENSION+")");
				continue;
			}
			try {
				reader = new LineNumberReader(new FileReader(file));
				long last = System.currentTimeMillis(), now = last, first = last;
				log.info("Read "+reader.getLineNumber()+" lines "+now+" 0 lps");
				log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
				
				for (String line; (line=reader.readLine())!= null;) {
					if( (now = System.currentTimeMillis()) - last > 2000)  {
						log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
						last = now;
						log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
					}
					line = line.trim();

					properties.put( NODENAME_KEY, line );
					long node = inserter.createNode( properties );
					nodes.add(node, properties);

				}
				now = System.currentTimeMillis();
				log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
				log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

			} catch (FileNotFoundException e) { log.error("Couldn't find file "+file+";",e);
			} catch (IOException e) { log.error("Trouble reading file "+file+";",e);
			} finally {
				if (reader!=null)
					try {
						reader.close();
					} catch (IOException e) { log.error("Trouble closing file "+file+";",e); }
			}
		}

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
		LineNumberReader reader=null;
		try {
			reader = new LineNumberReader(new FileReader(graphFile));

			String line;
			long last = System.currentTimeMillis(), now = last, first = last;
			log.info("Read "+reader.getLineNumber()+" lines "+now+" 0 lps");
			log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
			while ((line=reader.readLine())!= null) {
				if(reader.getLineNumber() % 20000 == 0) { tx.success(); tx.finish(); tx = graphDb.beginTx(); }
				if( (now = System.currentTimeMillis()) - last > 2000)  {
					log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
					last = now;
					log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
				}
				line=line.trim();
				if (line.startsWith("#") || line.length()==0) continue;
				String[] parts = line.split("\t");
				if (parts.length != 3) throw new IllegalStateException("Bad line "+reader.getLineNumber()+" (must be $edge\t$src\t$dst): "+line);
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
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			tx.finish();
			if (reader!=null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
