package edu.cmu.ml.praprolog.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * File utility with support for automatically skipping blank lines and #-comments.
 * 
 * See Also RawPosNegExampleStreamer, which shares the same comment syntax.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class ParsedFile implements Iterable<String>, Iterator<String> {
	private static final Logger log = Logger.getLogger(ParsedFile.class);
	private static final boolean DEFAULT_STRICT=true;
	private boolean cheating=false;
	private String filename;
	private LineNumberReader reader;
	private String peek;
	private int dataLine=-2;
	private boolean closed;
	private boolean strict;
	public ParsedFile(String filename) {
		this(filename,DEFAULT_STRICT);
	}
	public ParsedFile(String filename, boolean strict) {
		this.strict = strict;
		this.init(filename);
	}
	
	public ParsedFile(File file) {
		this(file,DEFAULT_STRICT);
	}
	public ParsedFile(File file, boolean strict) {
		this.strict=strict;
		try {
			this.init(file.getCanonicalPath());
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private void init(String filename) {
		this.filename = filename;
		try {
			reader = new LineNumberReader(new FileReader(filename));
			closed = false;
			this.next();
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Used primarily for unit tests (only supports up to 1024 bytes)
	 * @param stringReader
	 */
	public ParsedFile(StringReader stringReader) {
		this.filename = stringReader.getClass().getCanonicalName() + stringReader.hashCode();
		this.reader = new LineNumberReader(stringReader);

		this.cheating=true;
		try {
			this.reader.mark(1024);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.next();
	}

	public void parseError() {
		parseError(null);
	}
	public void parseError(String msg) {
		if (this.strict)
			throw new IllegalArgumentException("Unparsable line "+filename+":"+reader.getLineNumber()+":"
					+ (msg!=null ? ("\n"+msg) : "")
					+ "\n"+peek);
		log.error("Unparsable line "+filename+":"+reader.getLineNumber()+":"
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

	public String getFileName() {
		return this.filename;
	}
	
	/** Reset the iterator back to the beginning of the file */
	public void reset() {
		if (this.cheating) {
			try {
				this.reader.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			this.close();
			this.init(this.filename);
		}
	}

}
