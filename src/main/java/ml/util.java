package ml;

import java.util.List;

public class util {

	static int factMult(int f, int c) {
		if (f >= c)
			return c;

		return f * factMult(f + 1, c);
	}

	static int comb(int k, int n) {
		if (n - k > 50)
			return Integer.MAX_VALUE;

		return factMult(n - k + 1, n) / fact(k);
	}

	static double cosSim(double[] a, double[] b) {
		double dotp = 0, maga = 0, magb = 0;

		for (int i = 0; i < a.length; i++) {
			dotp += a[i] * b[i];
			maga += Math.pow(a[i], 2);
			magb += Math.pow(b[i], 2);
		}

		maga = Math.sqrt(maga);
		magb = Math.sqrt(magb);
		double d = dotp / (maga * magb);

		if (d == Double.NaN)
			d = 0;
		return d == Double.NaN ? 0 : d;
	}

	static int fact(int a) {
		return factMult(1, a);
	}

	static double idf(List<String[]> docs, String term) {
		double n = 0;
		for (String[] x : docs)
			for (String s : x)
				if (s.equalsIgnoreCase(term)) {
					n++;
					break;
				}
		return Math.log(docs.size() / n);
	}

	static double[] normalizeVector(double[] inputV) {
		double[] outputV = new double[inputV.length];
		double sq = 0;
		for (double d : inputV)
			sq += Math.pow(d, 2);
		double norm = Math.sqrt(sq);
		for (int i = 0; i < inputV.length; i++)
			outputV[i] = inputV[i] / norm;
		return outputV;
	}

	static String[] splitText(String input) {
		return input.toString().replaceAll("[\\W&&[^\\s]]", "").split("\\W+");
	}

	static double tf(String[] doc, String term) {
		double n = 0;
		for (String s : doc)
			if (s.equalsIgnoreCase(term))
				n++;
		return n / doc.length;
	}
}
