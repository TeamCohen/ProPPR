package edu.cmu.ml.praprolog.prove;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.ml.praprolog.util.ParsedFile;

/**
 * Yields a RawPosNegExample for each line in a file. The format for each line is:
             queryGoal <TAB> +posGroundGoal <TAB> ... <TAB> -negGroundGoal
   where a goal is in the space-delimited format
   			 functor arg1 arg2 ...
 * @author wcohen,krivard
 *
 */
public class RawPosNegExampleStreamer {
	private String[] files;
	public RawPosNegExampleStreamer(String ... files) {
		this.files = files;
	}

	public RawPosNegExample exampleFromString(String line, boolean parsed) {
		if (!parsed) {
			//hack
			line = line.replaceAll("[(,)]", " ").replaceAll("\\)","");
		}
		String[] parts = line.trim().split("\t");

		Goal query = Goal.parseGoal(parts[0]);

		List<String> posList = new ArrayList<String>();
		List<String> negList = new ArrayList<String>();
		for (int i=1; i<parts.length; i++) {
			if (parts[i].startsWith("+")) {
				posList.add(parts[i].substring(1));
			} else if (parts[i].startsWith("-")) {
				negList.add(parts[i].substring(1));
			}
		}
		return new RawPosNegExample(query,posList,negList);
	}

	public List<RawPosNegExample> load() {
		List<RawPosNegExample> examples = new ArrayList<RawPosNegExample>();
		for (String f : files) {
			boolean parsed = f.endsWith(".cdata");
			ParsedFile file = new ParsedFile(f);
			for(String line : file) {
				examples.add(exampleFromString(line,parsed));
			}
		}
		return examples;
	}

	public Iterable<RawPosNegExample> stream() {
		return new RawExampleIterator(files);
	}

	/**
	 * See Also ParsedFile for comment syntax
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	public class RawExampleIterator implements Iterable<RawPosNegExample>, Iterator<RawPosNegExample> {
		LineNumberReader reader=null;
		String currentFile;
		String[] fileList;
		int currentFileId;
		String nextLine = null;
		Exception lastException=null;
		boolean parsed;
		public RawExampleIterator(String[] files) {
			currentFileId=-1;
			fileList = files;
			nextFile();
		}
		protected void nextFile() {
			currentFileId++;
			try {
				if (reader != null) reader.close();

				if (currentFileId < fileList.length) {
					currentFile=fileList[currentFileId];
					parsed = currentFile.endsWith(".cdata");
					reader = new LineNumberReader(new FileReader(currentFile));
					peek();
				}
			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		protected void peek() throws IOException {
			nextLine = reader.readLine();
			if (nextLine == null) nextFile();
			nextLine = nextLine.trim();
			if (nextLine.isEmpty() || nextLine.startsWith("#")) peek();
		}

		@Override
		public Iterator<RawPosNegExample> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			if (lastException != null) throw new IllegalStateException(lastException);
			return nextLine != null;
		}

		@Override
		public RawPosNegExample next() {
			RawPosNegExample next = exampleFromString(nextLine,parsed);
			try {
				peek();
			} catch(IOException e) {
				lastException = e;
			}
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Can't remove from a file-backed iterator");
		}

	}
}
