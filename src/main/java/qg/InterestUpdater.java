package qg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ta.Acquisition;
import conf.Interest;
import conf.Phrase;

public class InterestUpdater {
	static final Logger logger = LogManager.getLogger(InterestUpdater.class
			.getName());

	private static InterestUpdater instance = null;
	private static double newPhraseMinSup;

	public static synchronized InterestUpdater getInstance() {
		if (instance == null)
			instance = new InterestUpdater();

		return instance;
	}

	public static void update(Interest interest) {
		if (Acquisition.getCurrentWindow().getStatistics()
				.getRelevantTweetCount() == 0)
			return;

		Collection<Phrase> phrases = Acquisition.getQuery().getPhrases()
				.values();
		synchronized (phrases) {
			for (Phrase phrase : phrases) {
				int totalIrrelevantTweetCount = phrase.getStatistics()
						.getTotalIrrelevantTweetCount();
				int totalRelevantTweetCount = phrase.getStatistics()
						.getTotalRelevantTweetCount();

				if (totalIrrelevantTweetCount == 0
						&& totalRelevantTweetCount > 0
						&& phrase.getTerms().length > 1) {
					Phrase newPhrase = generalizePhrase(interest, phrase);
					logger.info("generalized phrase: " + phrase.getText()
							+ " to " + newPhrase.getText());
					interest.removePhrase(phrase);
					interest.addPhrase(newPhrase);
				} else if (totalIrrelevantTweetCount > 0
						&& totalRelevantTweetCount == 0) {
					interest.removePhrase(phrase);
					logger.info("removed irrelevant phrase: "
							+ phrase.getText());
				} else if (totalIrrelevantTweetCount == 0
						&& totalRelevantTweetCount == 0) {
					if (phrase.getStatistics().getStatCount() > 3) {
						interest.removePhrase(phrase);
						logger.info("removed unchanged phrase: "
								+ phrase.getText());
					}
				} else {
					Phrase newPhrase = specializePhrase(interest, phrase);
					logger.info("specialize phrase: " + phrase.getText()
							+ " to " + newPhrase.getText());
					interest.removePhrase(phrase);
					interest.addPhrase(newPhrase);
				}
			}

			addNewPhrases(interest);
		}

		interest.computeFrequencies();
	}

	private static void addNewPhrases(Interest interest) {
		Map<Phrase, Double> newPhrases = new HashMap<Phrase, Double>();

		Map<String, Integer> frequentRelevantPatterns = Acquisition
				.getCurrentWindow().getStatistics()
				.getFrequentRelevantPatterns();
		for (String fpattern : frequentRelevantPatterns.keySet()) {
			double freq = (double) frequentRelevantPatterns.get(fpattern)
					/ (double) Acquisition.getCurrentWindow().getStatistics()
							.getRelevantTweetCount();
			if (freq > newPhraseMinSup) {
				Phrase newPhrase = new Phrase();
				newPhrase.setText(fpattern.replace(",", " "));
				newPhrase.setWeight(freq);
				newPhrase.setInitial(false);
				double score = 0;
				for (String term : newPhrase.getTerms()) {
					double interestTermFreq = interest.getPhraseTermFreq(term);
					double termSpecificity = 1 - Acquisition
							.getTermCommonness(term);
					double termRelevanceToInterest = interestTermFreq == 0 ? interest
							.getMinPhraseTermFreq() : interestTermFreq;
					score += termSpecificity * termRelevanceToInterest;
					logger.info(term + " " + termSpecificity + " "
							+ termRelevanceToInterest);
				}

				score = score / (double) newPhrase.getTerms().length;
				newPhrases.put(newPhrase, score);
			}
		}

		List<Entry<Phrase, Double>> newPhrasesEntrySet = new ArrayList<Map.Entry<Phrase, Double>>(
				newPhrases.entrySet());
		Collections.sort(newPhrasesEntrySet,
				new Comparator<Entry<Phrase, Double>>() {
					public int compare(Entry<Phrase, Double> arg0,
							Entry<Phrase, Double> arg1) {
						return arg1.getValue().compareTo(arg0.getValue());
					}
				});

		for (int i = 0; i < (newPhrasesEntrySet.size() > 10 ? 10
				: newPhrasesEntrySet.size()); i++) {
			logger.info("to be added phrase: "
					+ newPhrasesEntrySet.get(i).getKey().getText() + " score: "
					+ newPhrasesEntrySet.get(i).getValue());
		}

		if (newPhrasesEntrySet.size() > 0) {
			interest.addPhrase(newPhrasesEntrySet.get(0).getKey());
			logger.info("added phrase: "
					+ newPhrasesEntrySet.get(0).getKey().getText() + " score: "
					+ newPhrasesEntrySet.get(0).getValue());
		}
	}

	private static Phrase generalizePhrase(Interest interest, Phrase phrase) {
		Phrase np = new Phrase();
		String npt = "";
		double mintf = 0;
		String term_mintf = "";
		String[] terms = phrase.getTerms();

		for (int i = 0; i < terms.length; i++) {
			String term = terms[i];
			double phraseTermFreq = interest.getPhraseTermFreq(term);
			double tf = 1 / (phraseTermFreq == 0 ? 1 : (double) phraseTermFreq);
			tf = tf * Acquisition.getTermCommonness(term);
			if (tf < mintf || mintf == 0) {
				mintf = tf;
				term_mintf = term;
			}
		}

		for (String term : terms) {
			if (!term.equalsIgnoreCase(term_mintf))
				npt += term + " ";
		}

		np.setText(npt);
		return np;
	}

	private static Phrase specializePhrase(Interest interest, Phrase phrase) {
		Phrase newPhrase = null;
		Map<String, Integer> frequentRelevantPatterns = Acquisition
				.getCurrentWindow().getStatistics()
				.getFrequentRelevantPatterns();

		double maxFreq = 0;
		for (String fpattern : frequentRelevantPatterns.keySet()) {
			double freq = (double) frequentRelevantPatterns.get(fpattern)
					/ (double) Acquisition.getCurrentWindow().getStatistics()
							.getRelevantTweetCount();
			if (freq > newPhraseMinSup && (freq > maxFreq || maxFreq == 0)) {
				newPhrase = new Phrase();
				newPhrase.setText(fpattern.replace(",", " "));
				newPhrase.setWeight(freq);
				newPhrase.setInitial(false);
				if (newPhrase.coverPhrase(phrase.getText())) {
					maxFreq = freq;
				}
			}
		}

		if (newPhrase == null) {
			for (Phrase p : interest.getPhrases()) {
				if (p.coverPhrase(phrase.getText()))
					newPhrase = p;
			}

			if (newPhrase == null)
				newPhrase = phrase;
		}

		return newPhrase;
	}

	// private static Report generateReport(Interest interest,
	// HashMap<String, Integer> terms,
	// HashMap<Integer, HashMap<String, Double>> itemsets) {
	// Report report = new Report();
	//
	// report.setStartTime(Acquisition.getCurrentWindow().getStartTime());
	// report.setEndTime(Acquisition.getCurrentWindow().getEndTime());
	// report.setInterestId(interest.getId());
	// report.setSingles(terms);
	// report.setItemsets(itemsets);
	// report.setTweetCount(interest.getStatistics().getLastWindowStatistics()
	// .getRelevantTweetCount());
	// return report;
	// }
}
