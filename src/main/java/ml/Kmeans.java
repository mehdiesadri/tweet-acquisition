package ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

public class Kmeans {
	private List<double[]> vecSpace;
	private int maxIter = 200;
	private int maxInit = 10;

	public HashMap<double[], TreeSet<Integer>> cluster(List<double[]> input,
			int k) {
		vecSpace = input;
		double bestSumSim = 0;

		HashMap<double[], TreeSet<Integer>> bestClusters = new HashMap<double[], TreeSet<Integer>>();
		HashMap<double[], TreeSet<Integer>> tempClusters = new HashMap<double[], TreeSet<Integer>>();
		HashMap<double[], TreeSet<Integer>> stepClusters = new HashMap<double[], TreeSet<Integer>>();

		List<Integer> rand = new ArrayList<Integer>();

		int numDocs = vecSpace.size();
		maxInit = Math.min(util.comb(k, numDocs), maxInit);

		for (int init = 0; init < maxInit; init++) {
			tempClusters.clear();
			stepClusters.clear();
			rand.clear();

			while (rand.size() < k)
				rand.add((int) (Math.random() * numDocs));

			for (int r : rand) {
				double[] temp = new double[vecSpace.get(r).length];
				System.arraycopy(vecSpace.get(r), 0, temp, 0, temp.length);
				stepClusters.put(temp, new TreeSet<Integer>());
			}

			boolean go = true;
			int iter = 0;
			while (go) {
				tempClusters = new HashMap<double[], TreeSet<Integer>>(
						stepClusters);
				assignVectors(tempClusters);
				stepClusters.clear();
				stepClusters = updateCentroids(tempClusters);

				// check for divergence
				String oldcent = "", newcent = "";
				for (double[] x : tempClusters.keySet())
					oldcent += Arrays.toString(x);

				for (double[] x : stepClusters.keySet())
					newcent += Arrays.toString(x);

				if (oldcent.equals(newcent))
					go = false;

				if (++iter >= maxIter)
					go = false;
			}

			double sumsim = 0;
			for (double[] c : tempClusters.keySet()) {
				TreeSet<Integer> cl = tempClusters.get(c);
				for (int vi : cl) {
					sumsim += util.cosSim(c, vecSpace.get(vi));
				}
			}

			if (sumsim > bestSumSim) {
				bestSumSim = sumsim;
				bestClusters.clear();
				bestClusters.putAll(tempClusters);
			}
		}

		return bestClusters;
	}

	private void assignVectors(HashMap<double[], TreeSet<Integer>> clusters) {
		for (int i = 0; i < vecSpace.size(); i++) {
			HashMap<double[], Double> distances = new HashMap<double[], Double>();
			for (double[] c : clusters.keySet()) {
				double csim = util.cosSim(c, vecSpace.get(i));
				distances.put(c, csim);
			}

			List<Entry<double[], Double>> sortedDistances = sort_asc(distances);
			clusters.get(sortedDistances.get(0).getKey()).add(i);

			for (int j = 1; j < sortedDistances.size(); j++) {
				double diff = sortedDistances.get(j - 1).getValue()
						- sortedDistances.get(j).getValue();
				double threshold = sortedDistances.get(j - 1).getValue() * .01;

				if (diff < threshold)
					clusters.get(sortedDistances.get(j).getKey()).add(i);
			}

		}
	}

	private List<Entry<double[], Double>> sort_asc(Map<double[], Double> clTerms) {
		List<Entry<double[], Double>> entries = new ArrayList<Entry<double[], Double>>();
		entries.addAll(clTerms.entrySet());
		Collections.sort(entries, new Comparator<Entry<double[], Double>>() {
			public int compare(Entry<double[], Double> o1,
					Entry<double[], Double> o2) {
				return (int) (o2.getValue() * Double.MAX_VALUE - o1.getValue()
						* Double.MAX_VALUE);
			}
		});

		return entries;
	}

	private HashMap<double[], TreeSet<Integer>> updateCentroids(
			HashMap<double[], TreeSet<Integer>> clusters) {
		HashMap<double[], TreeSet<Integer>> step = new HashMap<double[], TreeSet<Integer>>();

		for (double[] cent : clusters.keySet()) {
			double[] updatec = new double[cent.length];
			for (int d : clusters.get(cent)) {
				double[] doc = vecSpace.get(d);
				for (int i = 0; i < updatec.length; i++)
					updatec[i] += doc[i];
			}
			for (int i = 0; i < updatec.length; i++)
				updatec[i] /= clusters.get(cent).size();
			step.put(updatec, new TreeSet<Integer>());
		}

		return step;
	}
}
