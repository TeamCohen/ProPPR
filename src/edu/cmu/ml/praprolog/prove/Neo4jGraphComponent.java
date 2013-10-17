package edu.cmu.ml.praprolog.prove;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

public class Neo4jGraphComponent extends GraphlikeComponent {
	private static final Logger log = Logger.getLogger(Neo4jGraphComponent.class);
	//	private static enum RelTypes implements RelationshipType {
	//	    FUNCTOR
	//	}
	private static final String NODENAME_KEY = "name";
	private static final String FUNCTOR_KEY = "name";

	private static GraphDatabaseService graphDb;
	private static Index<Node> nodeIndex;
	private static RelationshipIndex relIndex;

	private static void init(String dbPath) {
		if (graphDb != null) return;
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath );
		nodeIndex = graphDb.index().forNodes( "nodes" );
		relIndex = graphDb.index().forRelationships( "relationships" );
		registerShutdownHook( graphDb );
	}

	public Neo4jGraphComponent(String dbPath) {
		init(dbPath);
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
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			System.err.println("Usage:\n\tdbPath graphFile functor arg1\n");
			System.exit(0);
		}

		String dbPath = args[0],
				graphFile = args[1],
				functor = args[2],
				arg1 = args[3];
		long t0 = System.currentTimeMillis();
		Neo4jGraphComponent nc = new Neo4jGraphComponent(dbPath);
		long t1 = System.currentTimeMillis();
		System.out.println("open "+dbPath+" "+(t1-t0));
		t1 = System.currentTimeMillis();
		nc.load(nc, graphFile);
		long t2 = System.currentTimeMillis();
		System.out.println("load "+graphFile+" "+(t2-t1));
		t2 = System.currentTimeMillis();
		for (Argument a : nc._indexGet(functor, new ConstantArgument(arg1))) {
			System.out.println("next item "+(System.currentTimeMillis()-t2));
			System.out.println(a);
			t2 = System.currentTimeMillis();
		}

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
		try {
			LineNumberReader reader = new LineNumberReader(new FileReader(graphFile));

			String line;
			long last = System.currentTimeMillis(), now = last, first = last;
			log.info("Read "+reader.getLineNumber()+" lines "+now+" "+((double) reader.getLineNumber() / (now-first) * 1000)+" lps");
			log.debug("mem "+now+" "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
			while ((line=reader.readLine())!= null) {
				if(reader.getLineNumber() % 10000 == 0) { tx.success(); tx.finish(); tx = graphDb.beginTx(); }
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
			log.debug("mem "+Runtime.getRuntime().totalMemory()+" "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

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
