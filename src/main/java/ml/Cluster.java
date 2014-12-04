package ml;

import java.util.ArrayList;
import java.util.List;

public class Cluster {

	private int numDims;
	private List<String> docIds;
	private List<Double> sumVs;
	private List<Double> normSumVs;
	private List<Double> weightedSumVs;
	private List<List<Double>> coreNodes;
	private double avgRelevance;
	private Cluster mergedInto;

	public Cluster(int numDims) {
		this.numDims = numDims;
		docIds = new ArrayList<String>();
		sumVs = new ArrayList<Double>();
		normSumVs = new ArrayList<Double>();
		weightedSumVs = new ArrayList<Double>();
		coreNodes = new ArrayList<List<Double>>();
	}

	public void addCoreNode(List<Double> coreNode) {
		this.coreNodes.add(coreNode);
	}

	public void addDimension() {
		numDims++;
		sumVs.add((double) 0);
		normSumVs.add((double) 0);
		weightedSumVs.add((double) 0);

		for (List<Double> node : coreNodes)
			node.add((double) 0);
	}

	public void addDoc(String docId, double rel) {
		avgRelevance = (avgRelevance * (double) docIds.size() + rel)
				/ ((double) docIds.size() + 1);
		docIds.add(docId);
	}

	public void removeDoc(String docId) {
		docIds.remove(docId);
	}

	public double getMbs() {
		double mbs = 0;
		double[] normWeightedSumVs = util.normalizeVector(getWeightedSumVs());
		for (int i = 0; i < normWeightedSumVs.length; i++)
			mbs += (normWeightedSumVs[i] * sumVs.get(i)) / getNumDocs();

		return mbs;
	}

	public double[] getSumV() {
		double[] c = new double[sumVs.size()];
		for (int i = 0; i < sumVs.size(); i++)
			c[i] = sumVs.get(i);

		return c;
	}

	public List<List<Double>> getCoreNodes() {
		return coreNodes;
	}

	public int getNumDims() {
		return numDims;
	}

	public int getNumDocs() {
		return getDocIds().size();
	}

	public void setSumV(double[] sumVs) {
		this.sumVs.clear();
		for (double d : sumVs)
			this.sumVs.add(d);
	}

	public void setNumDims(int numDims) {
		this.numDims = numDims;
	}

	public List<String> getDocIds() {
		return docIds;
	}

	public double[] getNormSumVs() {
		double[] c = new double[normSumVs.size()];
		for (int i = 0; i < normSumVs.size(); i++)
			c[i] = normSumVs.get(i);

		return c;
	}

	public void setNormSumVs(List<Double> normSumVs) {
		this.normSumVs = normSumVs;
	}

	public void setNormSumVs(double[] normSumVs) {
		this.normSumVs.clear();
		for (double d : normSumVs)
			this.normSumVs.add(d);
	}

	public double[] getWeightedSumVs() {
		double[] c = new double[weightedSumVs.size()];
		for (int i = 0; i < weightedSumVs.size(); i++)
			c[i] = weightedSumVs.get(i);

		return c;
	}

	public void setWeightedSumVs(double[] weightedSumVs) {
		this.weightedSumVs.clear();
		for (double d : weightedSumVs)
			this.weightedSumVs.add(d);
	}

	public void setWeightedSumVs(List<Double> weightedSumVs) {
		this.weightedSumVs = weightedSumVs;
	}

	public double getAvgRelevance() {
		return avgRelevance;
	}

	public synchronized void merge(Cluster c) {
		c.mergedInto = this;
		avgRelevance = (avgRelevance * (double) getNumDocs() + c.avgRelevance
				* (double) c.getNumDocs())
				/ ((double) getNumDocs() + (double) c.getNumDocs());

		for (int i = 0; i < sumVs.size(); i++)
			sumVs.set(i, sumVs.get(i) + c.sumVs.get(i));

		for (int i = 0; i < normSumVs.size(); i++)
			normSumVs.set(i, normSumVs.get(i) + c.normSumVs.get(i));

		for (int i = 0; i < weightedSumVs.size(); i++)
			weightedSumVs.set(i, weightedSumVs.get(i) + c.weightedSumVs.get(i));

		coreNodes.addAll(c.coreNodes);
		docIds.addAll(c.docIds);
	}

	public Cluster getMergedInto() {
		if (mergedInto == null)
			return this;
		return mergedInto;
	}
}
