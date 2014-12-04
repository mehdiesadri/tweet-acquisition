package ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import conf.Interest;
import conf.Phrase;

public class Apriori {
	static final Logger logger = LogManager.getLogger(Apriori.class.getName());

	public String interestId;
	public HashMap<Integer, HashMap<String, Double>> itemsets;
	public HashMap<String, Integer> singles;

	private double numberOfTransactions;
	private double minsup;
	private Interest interest;

	public Apriori(double minsup, Interest interest) {
		this.interest = interest;
		this.numberOfTransactions = interest.getWeightSum();
		this.minsup = minsup;
		itemsets = new HashMap<Integer, HashMap<String, Double>>();
	}

	public void findAssociationRules(int maxLength) {
		itemsets.put(1, new HashMap<String, Double>());
		Map<String, Double> termFreq = interest.getPhraseTermFreq();

		for (Entry<String, Double> single : termFreq.entrySet()) {
			double sup = single.getValue() / interest.getWeightSum();
			if (sup >= minsup)
				itemsets.get(1).put(single.getKey(), single.getValue());
		}

		getFrequentItemsets(maxLength);
	}

	public void findAssociationRules(String interestId,
			HashMap<String, Integer> singles, int maxLength) {
		this.singles = singles;
		this.interestId = interestId;
		itemsets.put(1, new HashMap<String, Double>());
		for (Entry<String, Integer> single : singles.entrySet()) {
			double sup = (double) single.getValue() / numberOfTransactions;
			if (sup >= minsup)
				itemsets.get(1)
						.put(single.getKey(), (double) single.getValue());
		}

		getFrequentItemsets(maxLength);
	}

	private List<String> generateCandidates(HashMap<String, Double> input) {
		List<String> output = new ArrayList<String>();
		List<String> terms = new ArrayList<String>();
		for (Entry<String, Double> i : input.entrySet()) {
			String[] ts = i.getKey().split(" ");
			for (String t : ts) {
				terms.add(t.trim());
			}
		}

		for (String t : terms) {
			for (Entry<String, Double> i : input.entrySet()) {
				boolean valuable = true;
				String[] ts = i.getKey().split(" ");
				for (String y : ts) {
					if (t.equals(y)) {
						valuable = false;
						break;
					}
				}

				if (!valuable)
					break;

				String items = t + " ";
				for (String y : ts) {
					items += y + " ";
				}

				output.add(items.trim());
			}
		}

		return output;
	}

	private void getFrequentItemsets(int length) {
		if (!itemsets.containsKey(length - 1))
			getFrequentItemsets(length - 1);

		try {
			HashMap<String, Double> subItemsets = itemsets.get(length - 1);
			if (subItemsets == null)
				return;

			List<String> candidates = generateCandidates(subItemsets);
			HashMap<String, Double> finalItemsets = pruneCandidates(candidates);
			if (finalItemsets != null && finalItemsets.size() > 0)
				itemsets.put(length, finalItemsets);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Double getInterestCandidateCount(String candidate) {
		double count = 0;

		for (Phrase phrase : interest.getPhrases()) {
			String[] cterms = candidate.split(" ");
			int termCount = 0;

			for (String cterm : cterms) {
				for (String pterm : phrase.getTerms()) {
					if (cterm.equals(pterm)) {
						termCount++;
						break;
					}
				}
			}

			if (termCount == cterms.length)
				count += phrase.getWeight();
		}

		return count;
	}

	private HashMap<String, Double> pruneCandidates(List<String> candidates) {
		HashMap<String, Double> output = new HashMap<String, Double>();
		for (String candidate : candidates) {
			double count = getInterestCandidateCount(candidate);
			if (count / numberOfTransactions >= minsup)
				output.put(candidate, count);
		}

		return output;
	}
}