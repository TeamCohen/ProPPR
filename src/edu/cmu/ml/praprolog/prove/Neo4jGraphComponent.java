package edu.cmu.ml.praprolog.prove;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.RelationshipIndex;

public class Neo4jGraphComponent extends GraphlikeComponent {
	private static final Logger log = Logger.getLogger(Neo4jGraphComponent.class);
	private static enum RelTypes implements RelationshipType {
	    FUNCTOR
	}
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
			Node firstNode = graphDb.createNode();
			firstNode.setProperty( NODENAME_KEY, src.getName() );
	        nodeIndex.add( firstNode, NODENAME_KEY, src.getName() );
			Node secondNode = graphDb.createNode();
			secondNode.setProperty( NODENAME_KEY, dst.getName() );
	        nodeIndex.add( secondNode, NODENAME_KEY, src.getName() );
			 
			Relationship relationship = firstNode.createRelationshipTo( secondNode, RelTypes.FUNCTOR );
			relationship.setProperty( FUNCTOR_KEY, functor );
			relIndex.add( relationship, FUNCTOR_KEY, functor);
		    tx.success();
		}
		finally
		{
		    tx.finish();
		}
	}

	@Override
	protected boolean _indexContains(String functor) {
		return relIndex.get(FUNCTOR_KEY, functor).hasNext();
	}

	@Override
	protected List<Argument> _indexGet(String functor, Argument srcConst) {
        Node srcNode = nodeIndex.get( NODENAME_KEY, srcConst.getName() ).getSingle();
        srcNode.getRelationships(Direction.OUTGOING);
	}

	@Override
	protected int _indexGetDegree(String functor, Argument srcConst) {
        Node srcNode = nodeIndex.get( NODENAME_KEY, srcConst.getName() ).getSingle();
	}

	@Override
	protected Map<Goal, Double> getFeatureDict() {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

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
