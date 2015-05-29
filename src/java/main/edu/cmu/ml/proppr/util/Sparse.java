package edu.cmu.ml.proppr.util;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author wcohen
 *
 */
public class Sparse
{
	static public class Vector 
	{
		public int[] index;
		public float[] val;
		public Vector(int[] index, float[] val)
		{
			this.index = index.clone();
			this.val = val.clone();
		}
		public Vector(int n) 
		{
			this.index = new int[n];
			this.val = new float[n];
		}
	}
	
	static public class Matrix
	{
		public int[] index;
		public Vector[] val;
		public Matrix(int[] index, Vector[] val)
		{
			this.index = index.clone();
			this.val = val.clone();
		}
		public Matrix(int n) 
		{
			this.index = new int[n];
			this.val = new Vector[n];
		}
		public void sortIndex() {
			TreeMap<Integer,Vector> buf = new TreeMap<Integer,Vector>();
			for (int i=0; i<index.length; i++) {
				buf.put(index[i],val[i]);
			}
			int k=0;
			for (Map.Entry<Integer,Vector> e : buf.entrySet()) {
				index[k] = e.getKey().intValue();
				val[k] = e.getValue();
				k++;
			}
		}
	}
}

	/*
	static public class Tensor
	{
		public int[] index;
		public Matrix[] val;
		private int p; // index of first unused slot
		public Tensor() 
		{
			this(20);  // default initial size
		}
		public Tensor(int n) 
		{
			this.index = new int[n];
			this.val = new Matrix[n];
			p = 0; 
		}
		public void insert(int i,Matrix mat) 
		{
			growIfNeeded();
			index[p] = i;
			val[p] = mat;
			p++;
		}
		// ensure there's room for one more entry
		private void growIfNeeded()
		{
			if (p>=index.length)
			{
				int n1 = 2*index.length;
				int[] index1 = new int[n1];
				Matrix[] val1 = new Matrix[n1];
				for (int i=0; i<p; i++) 
				{
					index1[i] = this.index[i];
					val1[i] = this.val[i];
				}
				this.index = index1;
				this.val = val1;
			}
		}
	}
}
	*/


/*

		}
		public void set(int i,int j,float v)
		{
			grow();
			int k = this.n++;
			this.index[k]
			this.val[k] = v;
		}
	}

*/
