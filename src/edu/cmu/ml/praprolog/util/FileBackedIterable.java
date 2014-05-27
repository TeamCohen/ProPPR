package edu.cmu.ml.praprolog.util;

public interface FileBackedIterable {
	/** If file is exhausted, reset the iterator to the top of the file. **/
	public void wrap();
}
