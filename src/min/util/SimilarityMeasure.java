package min.util;
import org.junit.Test;

public class SimilarityMeasure
{
	@Test
	public void test()
	{
		double[] v1 = new double[]{0.31, 0.14, 0.27};
		double[] v2 = new double[]{0.37, 0.25, 0.19};
		double[] v3 = new double[]{0.31, 0.14, 0.27};
		System.out.println(SimilarityMeasure.JSDivergence(v1,v3));
		System.out.println(SimilarityMeasure.KLDivergence(v1, average(v1, v2)));
		System.out.println(SimilarityMeasure.KLDivergence(v2, average(v1, v2)));
	}
	
	/**
	 * The JS divergence similarity
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double JSDivergence(double[] v1, double[] v2)
	{
		double result = 0;
		result = 0.5 * (KLDivergence(v1, average(v1, v2)) + 
				KLDivergence(v2, average(v1, v2)));
		return result;
	}
	/**
	 * The JS divergence similarity
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double KLDivergence(double[] v1, double[] v2)
	{
		double result = 0;
		for(int i = 0; i < v1.length && i < v2.length; i++)
		{
			result += v1[i] * Math.log(v1[i] / v2[i]);
		}
		return result;
	}
	private static double[] average(double[] v1, double[] v2)
	{
		double[] result = new double[v1.length];
		for(int i = 0; i < result.length; i++)
		{
			result[i] = (v1[i] + v2[i]) / 2;
		}
		return result;
	}
	/**
	 * The normal cosine similarity
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double cosine(double[] v1, double[] v2)
	{
		double result = 0;
		result = dotProduct(v1, v2) / (moudle(v1) * moudle(v2));
		return result;
	}
	// compute the moudle of a vector
	private static double moudle(double[] v)
	{
		double result = 0;
		for(int i = 0; i < v.length; i++)
		{
			result += v[i] * v[i];
		}
		return Math.sqrt(result);
	}
	// dot product of two vectors
	private static double dotProduct(double[] v1, double[] v2)
	{
		double result = 0;
		for(int i = 0; i < v1.length && i < v2.length; i++)
		{
			result += v1[i] * v2[i];
		}
		return result;
	}
}
