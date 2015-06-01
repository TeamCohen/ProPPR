package edu.cmu.ml.proppr.util;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author wcohen
 *
 */

public class SimpleSparse
{
	static public class FloatVector 
	{
		public int[] index;
		public float[] val;
		public FloatVector(int[] index, float[] val)
		{
			this.index = index.clone();
			this.val = val.clone();
		}
		public FloatVector(int n) 
		{
			this.index = new int[n];
			this.val = new float[n];
		}
		public double dot(LongDense.AbstractFloatVector vec) 
		{
			double result = 0.0;
			for (int i=0; i<index.length; i++) {
				result += val[i]*vec.get(index[i]);
			}
			return result;
		}
	}
	
	static public class FloatMatrix
	{
		public int[] index;
		public FloatVector[] val;
		public FloatMatrix(int[] index, FloatVector[] val)
		{
			this.index = index.clone();
			this.val = val.clone();
		}
		public FloatMatrix(int n) 
		{
			this.index = new int[n];
			this.val = new FloatVector[n];
		}
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
