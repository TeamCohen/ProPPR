package edu.cmu.ml.proppr.util;

/**
 * @author wcohen
 *
 */

public class LongDenseVector
{
	public float[] val = new float[10];
	int maxIndex = 0;

	public int size() 
	{ 
		return maxIndex+1;
	}
	public double get(int k) 
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
	private void growIfNeededTo(int k) 
	{
		if (val.length <= k) {
			int n = Math.max(2*val.length, k+1);
			float[] tmp = new float[n];
			System.arraycopy(val,0, tmp,0, maxIndex+1);
			val = tmp;
		}
	}

	public static void main(String[] argv) throws Exception {
		LongDenseVector v = new LongDenseVector();
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
