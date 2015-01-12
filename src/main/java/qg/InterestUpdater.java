package qg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ta.Acquisition;
import conf.Interest;
import conf.Phrase;

public class InterestUpdater {
	private static final double percentageOfNewPhrasesToAdd = .3;
	private static final double minNewPhraseScore = .1;

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

				if (phrase.getStatistics().getStatCount() > 5) {
					if (totalIrrelevantTweetCount == 0
							&& totalRelevantTweetCount > 0
							&& phrase.getTerms().size() > 1) {
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
						interest.removePhrase(phrase);
						logger.info("removed unchanged phrase: "
								+ phrase.getText());
					} else {
						Phrase newPhrase = specializePhrase(interest, phrase);
						logger.info("specialize phrase: " + phrase.getText()
								+ " to " + newPhrase.getText());
						interest.removePhrase(phrase);
						interest.addPhrase(newPhrase);
					}
				}
			}

			addNewPhrases(interest);
		}

		interest.computeFrequencies();
	}

	private static void addNewPhrases(Interest interest) {
		Map<Phrase, Double> newPhrases = generatePotentialNewPhrases(interest);
		Map<Phrase, Double> newHashtagPhrases = generatePotentialNewHashtagPhrases();

		for (Phrase p : newHashtagPhrases.keySet())
			newPhrases.put(p, newHashtagPhrases.get(p));

		List<Entry<Phrase, Double>> newPhrasesEntrySet = new ArrayList<Map.Entry<Phrase, Double>>(
				newPhrases.entrySet());
		Collections.sort(newPhrasesEntrySet,
				new Comparator<Entry<Phrase, Double>>() {
					public int compare(Entry<Phrase, Double> arg0,
							Entry<Phrase, Double> arg1) {
						return arg1.getValue().compareTo(arg0.getValue());
					}
				});

		logger.info("potential new phrases: " + newPhrasesEntrySet);

		Integer index = 0;
		synchronized (index) {
			if (newPhrasesEntrySet.size() > 0) {
				for (int i = 0; i < Math.max(1,
						(int) (percentageOfNewPhrasesToAdd * newPhrasesEntrySet
								.size())); i++) {

					if (index >= newPhrasesEntrySet.size())
						break;

					Entry<Phrase, Double> p = newPhrasesEntrySet.get(index);
					while (interest.coverPhrase(p.getKey().getText())
							|| interest.phraseCovers(p.getKey())) {
						index++;
						if (index < newPhrasesEntrySet.size())
							p = newPhrasesEntrySet.get(index);
						else
							break;
					}

					if (index < newPhrasesEntrySet.size()) {
						interest.addPhrase(p.getKey());
						logger.info("added phrase: " + p.getKey().getText()
								+ " score: " + p.getValue());
					}
				}
			}
		}
	}

	private static Map<Phrase, Double> generatePotentialNewPhrases(
			Interest interest) {
		Map<Phrase, Double> potentialNewPhrases = new HashMap<Phrase, Double>();

		Map<String, Integer> frequentRelevantPatterns = Acquisition
				.getInterest().getStatistics().getFrequentRelevantPatterns();
		Map<String, Integer> frequentIrrelevantPatterns = Acquisition
				.getInterest().getStatistics().getFrequentIrrelevantPatterns();
		Map<String, Integer> frequentRelevantHashtags = Acquisition
				.getInterest().getStatistics().getFrequentRelevantHashtags();
		final Map<String, Integer> frequentIrrelevantHashtags = Acquisition
				.getInterest().getStatistics().getFrequentIrrelevantHashtags();

		for (String fpattern : frequentRelevantPatterns.keySet()) {
			double freq = (double) frequentRelevantPatterns.get(fpattern)
					/ (double) Acquisition.getInterest().getStatistics()
							.getTotalRelevantTweetCount();

			if (freq > newPhraseMinSup) {
				if (frequentIrrelevantPatterns.containsKey(fpattern)) {
					continue;
				}

				Phrase newPhrase = new Phrase();
				newPhrase.setText(fpattern.replace(",", " "));
				newPhrase.setInitial(false);
				double score = 0;

				for (String term : newPhrase.getTerms()) {
					double interestTermFreq = interest.getPhraseTermFreq(term);
					double termSpecificity = Acquisition
							.getTermCommonness(term);
					if (frequentRelevantHashtags.containsKey(term)) {
						double hashtagFreq = (double) (frequentRelevantHashtags
								.get(term) - (frequentIrrelevantHashtags
								.containsKey(term) ? frequentIrrelevantHashtags
								.get(term) : 0))
								/ (double) interest.getStatistics()
										.getTotalRelevantTweetCount();
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
				if (score > minNewPhraseScore)
					potentialNewPhrases.put(newPhrase, score);
			}
		}

		return potentialNewPhrases;
	}

	private static Map<Phrase, Double> generatePotentialNewHashtagPhrases() {
		Map<Phrase, Double> potentialNewHashtagPhrases = new HashMap<Phrase, Double>();

		Map<String, Integer> frequentRelevantHashtags = Acquisition
				.getInterest().getStatistics().getFrequentRelevantHashtags();
		final Map<String, Integer> frequentIrrelevantHashtags = Acquisition
				.getInterest().getStatistics().getFrequentIrrelevantHashtags();

		List<Entry<String, Integer>> newPhrasesHashtagsEntrySet = new ArrayList<Map.Entry<String, Integer>>(
				frequentRelevantHashtags.entrySet());
		Collections.sort(newPhrasesHashtagsEntrySet,
				new Comparator<Entry<String, Integer>>() {
					public int compare(Entry<String, Integer> arg0,
							Entry<String, Integer> arg1) {
						Integer value0 = arg0.getValue()
								- (frequentIrrelevantHashtags.containsKey(arg0
										.getKey()) ? frequentIrrelevantHashtags
										.get(arg0.getKey()) : 0);
						Integer value1 = arg1.getValue()
								- (frequentIrrelevantHashtags.containsKey(arg1
										.getKey()) ? frequentIrrelevantHashtags
										.get(arg1.getKey()) : 0);
						return value1.compareTo(value0);
					}
				});

		for (String ht : frequentRelevantHashtags.keySet()) {
			double score = frequentRelevantHashtags.get(ht)
					- (frequentIrrelevantHashtags.containsKey(ht) ? frequentIrrelevantHashtags
							.get(ht) : 0);
			score = score
					/ (double) (Acquisition.getInterest().getStatistics()
							.getTotalRelevantTweetCount());

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
		Phrase np = new Phrase();
		String npt = "";
		double mintf = 0;
		double minitf = 0;
		String term_mintf = "";
		HashSet<String> terms = phrase.getTerms();

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

		np.setText(npt);

		if (minitf > .2)
			return np;
		else
			return phrase;
	}

	private static Phrase specializePhrase(Interest interest, Phrase phrase) {
		Phrase newPhrase = null;
		Map<String, Integer> frequentRelevantPatterns = Acquisition
				.getCurrentWindow().getStatistics().getRelevantPatterns();

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
				if (phrase.coverPhrase(newPhrase.getText())) {
					maxFreq = freq;
				}
			}
		}

		if (newPhrase == null) {
			for (Phrase p : interest.getPhrases()) {
				if (phrase.coverPhrase(p.getText()))
					newPhrase = p;
			}

			if (newPhrase == null)
				newPhrase = phrase;
		}

		return newPhrase;
	}
}
