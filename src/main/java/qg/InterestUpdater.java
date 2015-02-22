package qg;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ta.Acquisition;
import util.MapUtil;
import util.SetUtil;
import conf.Interest;
import conf.Phrase;
import conf.Query;

public class InterestUpdater {
	static final Logger logger = LogManager.getLogger(InterestUpdater.class
			.getName());

	private static InterestUpdater instance = null;

	public static synchronized InterestUpdater getInstance() {
		if (instance == null)
			instance = new InterestUpdater();

		return instance;
	}

	public static void quickUpdate() {
		if (Acquisition.getCurrentWindow().getStatistics()
				.getRelevantTweetCount() == 0)
			return;

		Interest interest = Acquisition.getInterest();
		Query query = Acquisition.getQuery();
		Collection<Phrase> phrases = query.getPhrases().values();

		synchronized (phrases) {
			for (Phrase phrase : phrases) {
				int windowTweetCount = interest.getStatistics()
						.getLastWindowStatistics().getTotalTweetCount();
				int totalIrrelevantTweetCount = phrase.getStatistics()
						.getLastWindowStatistics().getIrrelevantTweetCount();
				int totalRelevantTweetCount = phrase.getStatistics()
						.getLastWindowStatistics().getRelevantTweetCount();

				if (((double) totalIrrelevantTweetCount > ((double) windowTweetCount * .01) && totalRelevantTweetCount == 0)
						|| ((double) totalRelevantTweetCount < ((double) totalIrrelevantTweetCount * .001))) {
					interest.removePhrase(phrase);
					logger.info("quickly removed irrelevant phrase: "
							+ phrase.getText());
				}
			}
		}

		interest.computeFrequencies();
	}

	public static void update() {
		if (Acquisition.getCurrentWindow().getStatistics()
				.getRelevantTweetCount() == 0)
			return;

		Interest interest = Acquisition.getInterest();

		if (interest.getStatistics().isFull()) {
			Collection<Phrase> phrases = Acquisition.getQuery().getPhrases()
					.values();
			synchronized (phrases) {
				for (Phrase phrase : phrases) {
					int totalIrrelevantTweetCount = phrase.getStatistics()
							.getIrrelevantTweetCount();
					int totalRelevantTweetCount = phrase.getStatistics()
							.getRelevantTweetCount();

					if (totalIrrelevantTweetCount == 0
							&& totalRelevantTweetCount > 0
							&& phrase.getTerms().size() > 1) {
						Phrase newPhrase = generalizePhrase(interest, phrase);
						if (newPhrase != phrase) {
							logger.info("generalized phrase: "
									+ phrase.getText() + " to "
									+ newPhrase.getText());
							interest.removePhrase(phrase);
							interest.addPhrase(newPhrase);
						}
					} else if (totalIrrelevantTweetCount > 0
							&& totalRelevantTweetCount == 0) {
						interest.removePhrase(phrase);
						logger.info("removed irrelevant phrase: "
								+ phrase.getText());
					} else if (totalIrrelevantTweetCount == 0
							&& totalRelevantTweetCount == 0) {
						interest.removePhrase(phrase);
						logger.info("removed unchanged phrase: "
								+ phrase.getText());
					} else {
						Phrase newPhrase = specializePhrase(interest, phrase);
						if (newPhrase != phrase) {
							logger.info("specialize phrase: "
									+ phrase.getText() + " to "
									+ newPhrase.getText());
							interest.removePhrase(phrase);
							interest.addPhrase(newPhrase);
						}

					}
				}
			}
		}

		addNewPhrases(interest);
		interest.computeFrequencies();
	}

	private static void addNewPhrases(Interest interest) {
		List<Entry<Phrase, Double>> newPhrasesEntrySet;
		Map<Phrase, Double> newPhrases = generatePotentialNewPhrases(interest);
		Map<Phrase, Double> newHashtagPhrases = generatePotentialNewHashtagPhrases();

		newPhrases.putAll(newHashtagPhrases);
		newPhrasesEntrySet = MapUtil.sortByValue(newPhrases);

		int count = 0;
		for (Entry<Phrase, Double> p : newPhrasesEntrySet) {
			if (interest.coverPhrase(p.getKey().getText())
					|| interest.phraseCovers(p.getKey()))
				continue;

			if (Acquisition.phraseLimit < interest.getPhrases().size()
					&& count > (int) (Acquisition.percentageOfNewPhrasesToAdd * Acquisition.phraseLimit))
				break;

			interest.addPhrase(p.getKey());
			count++;
			logger.info("added phrase: " + p.getKey().getText() + " score: "
					+ p.getValue());
		}
	}

	private static Map<Phrase, Double> generatePotentialNewPhrases(
			Interest interest) {
		Map<Phrase, Double> potentialNewPhrases = new HashMap<Phrase, Double>();

		Map<String, Integer> frequentRelevantPatterns = Acquisition
				.getInterest().getStatistics().getRelevantPatterns();
		Map<String, Integer> frequentIrrelevantPatterns = Acquisition
				.getInterest().getStatistics().getIrrelevantPatterns();
		Map<String, Integer> frequentRelevantHashtags = Acquisition
				.getInterest().getStatistics().getRelevantHashtags();
		Map<String, Integer> frequentIrrelevantHashtags = Acquisition
				.getInterest().getStatistics().getIrrelevantHashtags();

		for (String fpattern : frequentRelevantPatterns.keySet()) {
			double freq = (double) (frequentRelevantPatterns.get(fpattern))
					/ (double) Acquisition.getInterest().getStatistics()
							.getRelevantTweetCount();
			double irfreq = (double) (frequentIrrelevantPatterns
					.containsKey(fpattern) ? frequentIrrelevantPatterns
					.get(fpattern) : 0)
					/ (double) Acquisition.getInterest().getStatistics()
							.getRelevantTweetCount();

			freq = freq - irfreq;
			if (freq < Acquisition.newPhraseMinSup)
				continue;

			Phrase newPhrase = new Phrase();
			newPhrase.setText(fpattern.replace(",", " ").trim());
			newPhrase.setInitial(false);
			double score = 0;

			for (String term : newPhrase.getTerms()) {
				double interestTermFreq = interest.getPhraseTermFreq(term);
				double termSpecificity = 1 - Acquisition
						.getTermCommonness(term);

				if (frequentRelevantHashtags.containsKey(term)) {
					double hashtagFreq = (double) (frequentRelevantHashtags
							.get(term) - (frequentIrrelevantHashtags
							.containsKey(term) ? frequentIrrelevantHashtags
							.get(term) : 0))
							/ (double) interest.getStatistics()
									.getRelevantTweetCount();
					// logger.info("@@ " + term + " " + hashtagFreq + " "
					// + termSpecificity);
					if (hashtagFreq > termSpecificity)
						termSpecificity = hashtagFreq;
				}

				double termRelevanceToInterest = interestTermFreq == 0 ? interest
						.getMinPhraseTermFreq() : interestTermFreq;
				score += termSpecificity * termRelevanceToInterest;
			}

			score = score / (double) newPhrase.getTerms().size();
			newPhrase.setWeight(score);
			if (score > Acquisition.minNewPhraseScore)
				potentialNewPhrases.put(newPhrase, score);
		}

		return potentialNewPhrases;
	}

	private static Map<Phrase, Double> generatePotentialNewHashtagPhrases() {
		Map<Phrase, Double> potentialNewHashtagPhrases = new HashMap<Phrase, Double>();

		Map<String, Integer> frequentRelevantHashtags = Acquisition
				.getInterest().getStatistics().getRelevantHashtags();
		final Map<String, Integer> frequentIrrelevantHashtags = Acquisition
				.getInterest().getStatistics().getIrrelevantHashtags();

		for (String ht : frequentRelevantHashtags.keySet()) {
			double score = frequentRelevantHashtags.get(ht)
					- (frequentIrrelevantHashtags.containsKey(ht) ? frequentIrrelevantHashtags
							.get(ht) : 0);
			score = score
					/ (double) (Acquisition.getInterest().getStatistics()
							.getRelevantTweetCount());

			double termSpecificity = 1 - Acquisition.getTermCommonness(ht);
			score = score * .75 + .25
					* (termSpecificity == .5 ? .9 : termSpecificity);
			if ((score > .26 && termSpecificity > .995)
					|| (score > .35 && termSpecificity == .5)) {
				Phrase phrase = new Phrase(ht, score);
				potentialNewHashtagPhrases.put(phrase, score);
			}
		}

		return potentialNewHashtagPhrases;
	}

	private static Phrase generalizePhrase(Interest interest, Phrase phrase) {
		Map<String, Integer> relevantPatterns = Acquisition.getInterest()
				.getStatistics().getRelevantPatterns();
		Phrase np = null;
		String npt = "";
		double mintf = 0;
		double minitf = 0;
		String term_mintf = "";
		double maxFreq = 0;

		HashSet<String> terms = phrase.getTerms();
		List<Set<String>> patterns = SetUtil
				.getSubsets(terms, terms.size() - 1);

		for (Set<String> p : patterns) {
			String pattern = "";
			for (String t : p)
				pattern += t + ",";
			pattern = pattern.substring(0, pattern.length() - 1).trim();
			if (relevantPatterns.containsKey(pattern)) {
				double freq = (double) relevantPatterns.get(pattern)
						/ (double) Acquisition.getInterest().getStatistics()
								.getRelevantTweetCount();
				if (freq > Acquisition.newPhraseMinSup
						&& (freq > maxFreq || maxFreq == 0)) {
					maxFreq = freq;
					np = new Phrase();
					String newPhraseStr = pattern.replace(",", " ").trim();
					np.setText(newPhraseStr);
					np.setWeight(freq);
					np.setInitial(false);
				}
			}
		}

		if (np != null)
			return np;

		for (String term : terms) {
			double phraseTermFreq = interest.getPhraseTermFreq(term);
			double tf = (phraseTermFreq == 0 ? 1 : (double) phraseTermFreq)
					* (1 - Acquisition.getTermCommonness(term));
			if (tf < mintf || mintf == 0) {
				mintf = tf;
				minitf = phraseTermFreq;
				term_mintf = term;
			}
		}

		for (String term : terms) {
			if (!term.equalsIgnoreCase(term_mintf))
				npt += term + " ";
		}

		if (minitf > .2) {
			np = new Phrase();
			np.setText(npt);
			np.setInitial(false);
			np.setWeight(phrase.getWeight());
			return np;
		}
		return phrase;
	}

	private static Phrase specializePhrase(Interest interest, Phrase phrase) {
		Map<String, Integer> relevantPatterns = Acquisition.getInterest()
				.getStatistics().getRelevantPatterns();
		Phrase newPhrase = null;

		double maxFreq = 0;
		for (String fpattern : relevantPatterns.keySet()) {
			double freq = (double) relevantPatterns.get(fpattern)
					/ (double) Acquisition.getInterest().getStatistics()
							.getRelevantTweetCount();

			newPhrase = new Phrase();
			String newPhraseStr = fpattern.replace(",", " ").trim();
			newPhrase.setText(newPhraseStr);
			newPhrase.setWeight(freq);
			newPhrase.setInitial(false);

			if (freq > Acquisition.newPhraseMinSup
					&& (freq > maxFreq || maxFreq == 0)
					&& phrase.coverPhrase(newPhrase.getText())) {
				maxFreq = freq;
			}
		}

		if (maxFreq == 0)
			newPhrase = null;

		if (newPhrase == null) {
			for (Phrase p : interest.getPhrases()) {
				if (phrase.coverPhrase(p.getText()) && p != phrase)
					newPhrase = p;
			}

			if (newPhrase == null)
				newPhrase = phrase;
		}

		return newPhrase;
	}
}
