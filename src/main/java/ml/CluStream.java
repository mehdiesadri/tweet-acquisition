package ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.SetUtil;
import conf.Tweet;

public class CluStream {
	static final Logger logger = LogManager
			.getLogger(CluStream.class.getName());

	private static final int maxMicroClusterCount = 25;

	private int docCount;
	private Map<String, Integer> terms;
	private List<Cluster> clusters;

	public CluStream() {
		clusters = new ArrayList<Cluster>();
	}

	public synchronized void cluster(List<Tweet> input, int k) {
		List<String[]> docs = new ArrayList<String[]>();
		List<String> docIds = new ArrayList<String>();
		List<Double> docRelevances = new ArrayList<Double>();
		List<double[]> vecSpace = new ArrayList<double[]>();

		terms = new HashMap<String, Integer>();
		docCount = input.size();

		for (Tweet tweet : input) {
			for (String term : tweet.getTerms()) {
				terms.put(term,
						(terms.containsKey(term) ? terms.get(term) : 0) + 1);
			}

			String[] doc = tweet.getTerms().toArray(
					new String[tweet.getTerms().size()]);
			docs.add(doc);
			docIds.add(String.valueOf(tweet.getId()));
			docRelevances.add(tweet.getRelevance());
		}

		for (String[] doc : docs)
			vecSpace.add(vectorizeText(doc));

		Kmeans kmeans = new Kmeans();
		HashMap<double[], TreeSet<Integer>> bestClusters = kmeans.cluster(
				vecSpace, k);

		for (double[] c : bestClusters.keySet()) {
			TreeSet<Integer> clDocIndexes = bestClusters.get(c);
			Cluster cluster = new Cluster(c.length);
			cluster.setSumV(c);

			double[] normSumV = new double[terms.size()];
			double[] weightedSumV = new double[terms.size()];

			double sumCosSim = 0;

			for (int docIndex : clDocIndexes) {
				String docId = docIds.get(docIndex);
				double docRel = docRelevances.get(docIndex);
				cluster.addDoc(docId, docRel);

				double[] vector = vecSpace.get(docIndex);
				double[] nVector = util.normalizeVector(vector);

				for (int i = 0; i < nVector.length; i++)
					normSumV[i] += nVector[i];

				double cosSim = util.cosSim(c, vector);
				if (cosSim == 0)
					cosSim = Double.MIN_VALUE;
				sumCosSim += cosSim;

				for (int i = 0; i < nVector.length; i++)
					weightedSumV[i] += cosSim * vector[i];
			}

			for (int i = 0; i < normSumV.length; i++)
				normSumV[i] = normSumV[i] / clDocIndexes.size();

			for (int i = 0; i < weightedSumV.length; i++)
				weightedSumV[i] = weightedSumV[i]
						/ (clDocIndexes.size() * sumCosSim);

			cluster.setNormSumVs(normSumV);
			cluster.setWeightedSumVs(weightedSumV);

			getClusters().add(cluster);
		}
	}

	public synchronized void cluster(Tweet tweet) {
		docCount++;

		for (String term : tweet.getTerms()) {
			if (terms.containsKey(term)) {
				terms.put(term, terms.get(term) + 1);
			} else {
				// TODO: remove
				if (terms.size() >= 200)
					break;
				terms.put(term, 1);
				for (Cluster cluster : getClusters())
					cluster.addDimension();
			}
		}

		String[] termsArray = tweet.getTerms().toArray(
				new String[tweet.getTerms().size()]);
		double[] vector = vectorizeText(termsArray);
		String id = String.valueOf(tweet.getId());
		double relevance = tweet.getRelevance();
		cluster(vector, id, relevance);
	}

	public synchronized List<Cluster> getClusters() {
		return clusters;
	}

	public void print() {
		Map<String, Double> clTerms = new HashMap<String, Double>();

		for (Cluster cl : getClusters()) {
			String signature = "";
			double[] c = cl.getSumV();
			for (int i = 0; i < c.length; i++)
				clTerms.put(
						terms.keySet().toArray(
								new String[terms.keySet().size()])[i], c[i]);

			List<Entry<String, Double>> sortedClTerms = SetUtil
					.sort_asc(clTerms);
			for (Entry<String, Double> clTerm : sortedClTerms) {
				if (clTerm.getValue() > .01)
					signature += " " + clTerm.getKey();
			}

			System.out.println("[" + cl.getNumDocs() + "]	" + signature.trim());
		}
	}

	public synchronized double getRelevance(String text) {
		String[] docTerms = util.splitText(text);
		List<String> existingDocTerms = new ArrayList<String>();

		for (String docTerm : docTerms) {
			if (terms.containsKey(docTerm))
				existingDocTerms.add(docTerm);
		}

		String[] edt = new String[existingDocTerms.size()];
		for (int i = 0; i < existingDocTerms.size(); i++)
			edt[i] = existingDocTerms.get(i);

		double[] docVector = vectorizeText(edt);

		Cluster destCluster = null;
		double maxCosSim = 0;

		for (Cluster cluster : getClusters()) {
			double cosSim = util.cosSim(docVector, cluster.getWeightedSumVs());
			if (cosSim >= maxCosSim || maxCosSim == 0) {
				maxCosSim = cosSim;
				destCluster = cluster;
			}
		}

		return destCluster.getAvgRelevance();
	}

	private synchronized void cluster(double[] docVector, String docId,
			double docRel) {
		Cluster destCluster = null;
		double maxCosSim = 0;

		for (Cluster cluster : getClusters()) {
			double cosSim = util.cosSim(docVector, cluster.getWeightedSumVs());
			if (cosSim >= maxCosSim) {
				maxCosSim = cosSim;
				destCluster = cluster;
			}
		}

		if (destCluster == null)
			return;
		double mbs = destCluster.getMbs();

		if (maxCosSim < mbs) {
			// create a new cluster
			destCluster = new Cluster(terms.size());
			destCluster.addDoc(docId, docRel);
			destCluster.setSumV(docVector);
			destCluster.setWeightedSumVs(docVector);
			destCluster.setNormSumVs(util.normalizeVector(docVector));
			getClusters().add(destCluster);
			if (getClusters().size() > maxMicroClusterCount)
				merge();
		} else {
			double[] currentCentroid = destCluster.getSumV();
			double[] newCentroid = new double[currentCentroid.length];

			for (int i = 0; i < currentCentroid.length; i++) {
				newCentroid[i] = (currentCentroid[i] * destCluster.getNumDocs() + docVector[i])
						/ (double) destCluster.getNumDocs();
			}

			destCluster.setSumV(newCentroid);
			destCluster.addDoc(docId, docRel);
		}
	}

	private double[] vectorizeText(String[] input) {
		double[] vector = new double[terms.size()];
		int i = 0;
		for (Entry<String, Integer> e : terms.entrySet()) {
			double preIdf = (double) docCount / (double) e.getValue();
			vector[i] = util.tf(input, e.getKey()) * Math.log(preIdf);
			i++;
		}

		return vector;
	}

	private synchronized void merge() {
		List<ClusterPair> pairs = new LinkedList<CluStream.ClusterPair>();
		for (Cluster cluster1 : getClusters()) {
			for (Cluster cluster2 : getClusters())
				pairs.add(new ClusterPair(cluster1, cluster2));
		}

		Collections.sort(pairs, new Comparator<ClusterPair>() {
			public int compare(ClusterPair o1, ClusterPair o2) {
				double d = (o2.similarity - o1.similarity) * 100000;
				return (int) d;
			}
		});

		for (ClusterPair pair : pairs) {
			pair.c1.getMergedInto().merge(pair.c2);
			getClusters().remove(pair.c2);
			if (clusters.size() < .8 * maxMicroClusterCount)
				break;
		}
	}

	class ClusterPair {
		Cluster c1;
		Cluster c2;
		double similarity;

		public ClusterPair(Cluster c1, Cluster c2) {
			this.c1 = c1;
			this.c2 = c2;
			similarity = util.cosSim(c1.getWeightedSumVs(),
					c2.getWeightedSumVs());
		}
	}

}
