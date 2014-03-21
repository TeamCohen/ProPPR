package edu.cmu.ml.praprolog.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * File utility with support for automatically skipping blank lines and #-comments.
 * 
 * See Also RawPosNegExampleStreamer, which shares the same comment syntax.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class ParsedFile implements Iterable<String>, Iterator<String> {
	private String filename;
	private LineNumberReader reader;
	private String peek;
	private int dataLine=-2;
	private boolean closed = false;
	public ParsedFile(String filename) {
		this.filename = filename;
		try {
			reader = new LineNumberReader(new FileReader(filename));
			this.next();
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public ParsedFile(File file) {
		try {
			this.filename = file.getCanonicalPath();
			reader = new LineNumberReader(new FileReader(file));
			this.next();
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public ParsedFile(StringReader stringReader) {
		this.filename = stringReader.getClass().getCanonicalName() + stringReader.hashCode();
		this.reader = new LineNumberReader(stringReader);
		this.next();
	}

	public void parseError() {
		parseError(null);
	}
	public void parseError(String msg) {
		throw new IllegalArgumentException("Unparsable line "+filename+":"+reader.getLineNumber()+":"
				+ (msg!=null ? ("\n"+msg) : "")
				+ "\n"+peek);
	}
	
	@Override
	public void finalize() {
		this.close();
	}
	
	public void close() {
		if (reader != null)
			try {
				reader.close();
				this.closed = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public boolean isClosed() {
		return this.closed;
	}
	
	@Override
	public Iterator<String> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return peek != null;
	}
	
	/**
	 * The current line number, ignoring comments and blank lines.
	 * @return
	 */
	public int getLineNumber() {
		return dataLine;
	}
	
	/**
	 * The current line number, including comments and blank lines.
	 * @return
	 */
	public int getAbsoluteLineNumber() {
		return this.reader.getLineNumber()-1;
	}
	
	protected boolean isComment(String line) {
		return line.startsWith("#");
	}
	protected void processComment(String line) {}

	@Override
	public String next() {
		String next = peek;
		try {
			peek = reader.readLine();
			for(boolean skip=true; peek != null; ) {
				skip = skip && ((peek=peek.trim()).length() == 0);
				if (isComment(peek)) {
					skip = true;
					processComment(peek);
				}
				if (!skip) break;
				peek = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		dataLine++;
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Can't remove a line from a file, silly");
	}

}
