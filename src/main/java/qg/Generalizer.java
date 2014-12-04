package qg;

import java.util.HashMap;
import java.util.Map.Entry;

import ml.Apriori;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import conf.Interest;
import conf.Phrase;

public class Generalizer {
	static final Logger logger = LogManager.getLogger(Generalizer.class
			.getName());

	private static Generalizer instance = new Generalizer();

	public static void getGeneralInterest(Interest interest) {
		int maxLength = 3;
		double minsup = .05;

		Apriori apriori = new Apriori(minsup, interest);
		apriori.findAssociationRules(maxLength);
		HashMap<Integer, HashMap<String, Double>> itemsets = apriori.itemsets;

		for (int i = 1; i < maxLength + 1; i++) {
			if (!itemsets.containsKey(i))
				continue;
			for (Entry<String, Double> itemset : itemsets.get(i).entrySet()) {
				boolean hasPhrase = false;
				for (Phrase phrase : interest.getPhrases()) {
					if (phrase.equals(itemset.getKey())) {
						hasPhrase = true;
						break;
					}
				}

				if (!hasPhrase) {
					double wgt = itemset.getValue() / interest.getWeightSum();
					if (wgt > 1)
						wgt = 1;
					Phrase gPhrase = new Phrase(itemset.getKey(), wgt);
					gPhrase.setInitial(false);
					// interest.addPhrase(gPhrase);
					// System.out.println(itemset.getKey() + " " + wgt);
				}
			}
		}
	}

	public static Generalizer getInstance() {
		if (instance == null) {
			instance = new Generalizer();
		}
		return instance;
	}

	public static void main(String[] args) {
		Interest interest = new Interest("1", "dataworld");

		Phrase phrase1 = new Phrase("data science", .8);
		Phrase phrase2 = new Phrase("big data analysis", 1);
		Phrase phrase3 = new Phrase("social data", .6);
		Phrase phrase4 = new Phrase("database management", .9);

		interest.addPhrase(phrase1);
		interest.addPhrase(phrase2);
		interest.addPhrase(phrase3);
		interest.addPhrase(phrase4);

		interest.computeFrequencies();
		getGeneralInterest(interest);

		for (Phrase phrase : interest.getPhrases())
			System.out.println(phrase.getText() + " -" + phrase.getWeight());
	}
}
