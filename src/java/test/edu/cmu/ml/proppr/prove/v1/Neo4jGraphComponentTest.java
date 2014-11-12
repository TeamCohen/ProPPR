package edu.cmu.ml.proppr.prove.v1;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.v1.SparseGraphComponent;

public class Neo4jGraphComponentTest {

	@Test
	public void test() {
		String file = "foo/bar/baz/foofactor.name_arg1type_arg2.type.i";
		
		assertTrue(file.endsWith(SparseGraphComponent.INDEX_EXTENSION));
		String nodetype = file.substring(file.lastIndexOf(File.separatorChar)+1);
		nodetype = nodetype.substring(0,nodetype.lastIndexOf(SparseGraphComponent.INDEX_EXTENSION));
		assertEquals("foofactor.name_arg1type_arg2.type",nodetype);
	}

}
