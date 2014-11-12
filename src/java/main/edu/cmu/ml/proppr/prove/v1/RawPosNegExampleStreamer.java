package edu.cmu.ml.proppr.prove.v1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.cmu.ml.proppr.util.ParsedFile;

/**
 * Yields a RawPosNegExample for each line in a file. The format for each line is:
             queryGoal <TAB> +posGroundGoal <TAB> ... <TAB> -negGroundGoal
   where a goal is in the space-delimited format
   			 functor arg1 arg2 ...
 * @author wcohen,krivard
 *
 */
public class RawPosNegExampleStreamer {
	private File[] files;
	public RawPosNegExampleStreamer(File ... filelist) {
		this.files = filelist;
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
		for (File f : files) {
			boolean parsed = f.getName().endsWith(".cdata");
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
		File currentFile;
		File[] fileList;
		int currentFileId;
		String nextLine = null;
		Exception lastException=null;
		boolean parsed;
		private void init() {
			currentFileId=-1;
			nextFile();
		}
		public RawExampleIterator(File[] files) {
			fileList = files;
			init();
		}
		public RawExampleIterator(String[] filenames) {
			fileList = new File[filenames.length];
			for (int i=0; i<filenames.length; i++) fileList[i] = new File(filenames[i]);
			init();
		}
		protected void nextFile() {
			currentFileId++;
			try {
				if (reader != null) reader.close();

				if (currentFileId < fileList.length) {
					currentFile=fileList[currentFileId];
					parsed = currentFile.getName().endsWith(".cdata");
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
			if (nextLine == null) {
				nextFile();
				return;
			}
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
