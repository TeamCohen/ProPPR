package edu.cmu.ml.proppr.util.math;

import java.util.Map;
import java.util.TreeMap;

/**
 * Lightweight implementations of sparse vectors and matrices.
 *
 * @author wcohen
 */

public class SimpleSparse
{
	/** A vector of floats, internally represented as two parallel
	 * arrays, one (index) with the indices of the non-zero values, and
	 * one (val) with the corresponding values.
	 * 
	 * Conceptually this represents a vector V for for all i,
	 *  V[vec.index[i]] == vec.val[i]
	 *
	 * A typical use would be to * iterate over the non-zero values like
	 this:
	 *
	 * for (int i=0; i<vec.index.length; i++) 
	 *    doSomethingWith(index[i],vec.val[i]);
	 * 
	 */
	static public class FloatVector 
	{
		public int[] index;
		public float[] val;
		public FloatVector(int[] index, float[] val)
		{
			this.index = index.clone();
			this.val = val.clone();
		}
		/** Create a float vector that can store n
		 * non-zero values. 
		 **/
		public FloatVector(int n) 
		{
			this.index = new int[n];
			this.val = new float[n];
		}
		/** Return the dot product with a second vector
		 * which is a LongDense.FloatVector
		 */
		public double dot(LongDense.AbstractFloatVector vec, float dflt) 
		{
			double result = 0.0;
			for (int i=0; i<index.length; i++) {
				result += val[i]* (vec.definesIndex(index[i]) ? vec.get(index[i]) : dflt);
			}
			return result;
		}
	}
	
	/** A sparse matrix of floats.  The internal representation is
	 * analgous to a FloatVector except that M[mat.index[i]] is a row of
	 * the matrix which contains non-zeros, encoded as a
	 * SimpleSparse.FloatVector.
	 *
	 * A typical use would be
	 *
	 * for (int i=0; i<mat.index.length; i++) {
	 *    int r = mat.index[i];
	 *    SimpleSparse.FloatVector row = mat.val[i];
	 *    for (int j=0; j<row.index.length; j++) {
	 *        int c = row.index[i];
	 *        float M_rc = row[val];
	 *        // M_rc is value of mat[r][c]
	 *        doSomething(...);
	 *    }
	 *  }
	 */
	static public class FloatMatrix
	{
		public int[] index;
		public FloatVector[] val;
		public FloatMatrix(int[] index, FloatVector[] val)
		{
			this.index = index.clone();
			this.val = val.clone();
		}
		/** A FloatMatrix that can store n non-zero rows 
		 **/
		public FloatMatrix(int n) 
		{
			this.index = new int[n];
			this.val = new FloatVector[n];
		}
		/** Sort the index and value arrays so that Arrays.binarysearch(r,
		 * mat.index) will quickly return the location in mat.index[] of
		 * row r.
		 */
		public void sortIndex() {
			TreeMap<Integer,FloatVector> buf = new TreeMap<Integer,FloatVector>();
			for (int i=0; i<index.length; i++) {
				buf.put(index[i],val[i]);
			}
			int k=0;
			for (Map.Entry<Integer,FloatVector> e : buf.entrySet()) {
				index[k] = e.getKey().intValue();
				val[k] = e.getValue();
				k++;
			}
		}
	}
}
