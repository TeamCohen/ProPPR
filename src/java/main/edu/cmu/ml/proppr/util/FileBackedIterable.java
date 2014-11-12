package edu.cmu.ml.proppr.util;

public interface FileBackedIterable {
	/** If file is exhausted, reset the iterator to the top of the file. **/
	public void wrap();
}
