package edu.cmu.ml.proppr.util;

/**
 * @author wcohen
 *
 */

public class LongDense
{
	static public abstract class AbstractFloatVector {
		abstract public float get(int k); 
	}

	static public class UnitVector extends AbstractFloatVector {
		public float get(int k) { return 1.0f; } 
	}

	static public class FloatVector extends AbstractFloatVector {
		public float[] val;
		int maxIndex = 0;

		public FloatVector(int sizeHint) {
			val = new float[sizeHint];
		}
		public FloatVector() {
			this(10);
		}

		public int size() 
		{ 
			return maxIndex+1;
		}
		public void clear() 
		{
			for (int k=0; k<=maxIndex; k++) {
				val[k] = 0;
			}
		}
		public float get(int k) 
		{
			growIfNeededTo(k);
			return val[k];
		}
		public void inc(int k, double delta) 
		{
			growIfNeededTo(k);
			val[k] += delta;
			maxIndex = Math.max(maxIndex,k);
		}
		public void set(int k, double v) 
		{
			growIfNeededTo(k);
			val[k] = (float)v;
			maxIndex = Math.max(maxIndex,k);
		}
		private void growIfNeededTo(int k) 
		{
			if (val.length <= k) {
				int n = Math.max(2*val.length, k+1);
				float[] tmp = new float[n];
				System.arraycopy(val,0, tmp,0, maxIndex+1);
				val = tmp;
			}
		}
	}

	static public class ObjVector<T> {
		public Object[] val = new Object[10];
		int maxIndex = 0;

		public int size() 
		{ 
			return maxIndex+1;
		}
		public T get(int k) 
		{
			growIfNeededTo(k);
			return (T)val[k];
		}
		public void set(int k, T newval) 
		{
			growIfNeededTo(k);
			val[k] = newval;
			maxIndex = Math.max(maxIndex,k);
		}
		private void growIfNeededTo(int k) 
		{
			if (val.length <= k) {
				int n = Math.max(2*val.length, k+1);
				Object[] tmp = new Object[n];
				System.arraycopy(val,0, tmp,0, maxIndex+1);
				val = tmp;
			}
		}
	}

	public static void main(String[] argv) throws Exception {
		LongDense.FloatVector v = new LongDense.FloatVector();
		for (int i=0; i<argv.length; i+=2) {
			int k = Integer.parseInt(argv[i]);
			double d = Double.parseDouble(argv[i+1]);
			v.inc(k,d);
		}
		for (int i=0; i<v.size(); i++) {
			System.out.println(i + ":\t" + v.get(i));
		}
	}
}
