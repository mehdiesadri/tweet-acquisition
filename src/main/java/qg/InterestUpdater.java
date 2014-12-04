package qg;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ta.Acquisition;
import conf.ConfigMgr;
import conf.Interest;
import conf.Phrase;
import conf.Report;

public class InterestUpdater {
	static final Logger logger = LogManager.getLogger(InterestUpdater.class
			.getName());

	private static InterestUpdater instance = null;
	private static double newPhraseMinSup;
	private static double phraseRelevanceThreshold;
	private static int newPhraseMaxLength; // max n-gram to consider

	public InterestUpdater() {
		phraseRelevanceThreshold = Double
				.valueOf(ConfigMgr
						.readConfigurationParameter("AcquisitionPhraseRelevanceThreshold"));
		newPhraseMinSup = Double.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionNewPhraseMinSup"));
		newPhraseMaxLength = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionNewPhraseMaxLength"));
		getInstance();
	}

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
						&& totalRelevantTweetCount > 0) {
					String before = phrase.getText();
					phrase = generalizePhrase(interest, phrase);
					logger.info("generalized phrase: " + before + " to "
							+ phrase.getText());
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
					String before = phrase.getText();
					phrase = specializePhrase(interest, phrase);
					logger.info("specialize phrase: " + before + " to "
							+ phrase.getText());
				}
			}

			addNewPhrases(interest);
		}

		interest.computeFrequencies();
	}

	private static void addNewPhrases(Interest interest) {
		Map<Phrase, Double> newPhrases = new HashMap<Phrase, Double>();

		for (Entry<String, Integer> fpattern : Acquisition.getCurrentWindow()
				.getStatistics().getFrequentRelevantPatterns().entrySet()) {
			double freq = (double) fpattern.getValue()
					/ (double) Acquisition.getCurrentWindow().getStatistics()
							.getRelevantTweetCount();
			if (freq > newPhraseMinSup) {
				Phrase newPhrase = new Phrase();
				newPhrase.setText(fpattern.getKey().replace(",", " "));
				newPhrase.setWeight(freq);
				newPhrase.setInitial(false);
				newPhrases.put(newPhrase, freq);
			}
		}

		for (Phrase phrase : newPhrases.keySet()) {
			double freq = newPhrases.get(phrase);
			logger.info("to be added phrase: " + freq + " " + phrase.getText());
		}
	}

	private static Phrase generalizePhrase(Interest interest, Phrase phrase) {
		Phrase np = new Phrase();
		String npt = "";
		double mintf = 0;
		String term_mintf = "";
		String[] terms = phrase.getTerms();

		for (int i = 0; i < terms.length; i++) {
			double tf = interest.getPhraseTermFreq(terms[i]);
			if (tf < mintf || mintf == 0) {
				mintf = tf;
				term_mintf = terms[i];
			}
		}

		for (String term : terms)
			if (!term.equalsIgnoreCase(term_mintf))
				npt += term + " ";

		return phrase;
	}

	private static Phrase specializePhrase(Interest interest, Phrase phrase) {
		return phrase;
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
