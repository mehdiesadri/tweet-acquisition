package rc;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ta.UserStatistics;
import conf.Interest;
import conf.Phrase;
import conf.Tweet;

public class RelevanceCheck {
	static final Logger logger = LogManager.getLogger(RelevanceCheck.class
			.getName());

	public static double getRelevance(Phrase phrase, Interest interest) {
		// for the relevance check of new phrases
		double satisfaction = 0;

		HashSet<String> phraseTerms = phrase.getTerms();
		for (String pterm : phraseTerms) {
			pterm = pterm.trim();
			satisfaction += interest.getPhraseTermFreq(pterm);
		}

		if (phraseTerms.size() > 0)
			return satisfaction / ((double) phraseTerms.size());

		return 0;
	}

	public synchronized static double getRelevance(Tweet tweet,
			Interest interest) {
		double phraseRel = getPhraseRelevance(tweet, interest);
		double cluRel = getCluRelevance(tweet, interest);
		double userRel = getUserRelevance(tweet, interest);

		double[] rels = new double[] { phraseRel, cluRel, userRel };
		Arrays.sort(rels);

		double relevance = rels[2] + ((1 - rels[2]) * rels[1])
				+ (1 - rels[2] + ((1 - rels[2]) * rels[1])) * rels[0];
		// logger.info(String.format("%.2f", relevance) + "\t["
		// + String.format("%.2f", phraseRel) + "\t"
		// + (cluRel > 0 ? "** " : "   ") + String.format("%.2f", cluRel)
		// + "\t" + (userRel > 0 ? "** " : "   ")
		// + String.format("%.2f", userRel) + "]\t" + tweet.getTerms());

		return relevance > 1 ? 1 : relevance;
	}

	private static double getPhraseRelevance(Tweet tweet, Interest interest) {
		double maxSatisfaction = 0;
		double phraseRel = 0;

		Collection<Phrase> phrases = interest.getPhrases();
		synchronized (phrases) {
			for (Phrase phrase : phrases) {
				if (!phrase.isInitial())
					continue;
				// todo: check phrase relevance only after certain amount of
				// certainity
				double satisfaction = getSatisfaction(tweet, interest, phrase)
						* phrase.getWeight();
				if (satisfaction > maxSatisfaction)
					maxSatisfaction = satisfaction;
				else
					phraseRel += satisfaction;
			}
		}

		double subRelevance = 0;
		if (interest.getWeightSum() > 1)
			subRelevance = phraseRel / (double) (interest.getWeightSum() - 1);
		phraseRel = maxSatisfaction + subRelevance * (1 - maxSatisfaction);
		return phraseRel;
	}

	private static double getCluRelevance(Tweet tweet, Interest interest) {
		double rel = 0;
		double maxRel = 0;
		int count = 0;

		for (Phrase phrase : interest.getPhrases()) {
			if (tweet.containsPhrase(phrase.getText()))
				continue;

			Map<String, Integer> relevantPatterns = phrase.getStatistics()
					.getFrequentRelevantPatterns();
			for (String pattern : relevantPatterns.keySet()) {
				String p = pattern.replaceAll(",", " ").trim();
				boolean flag = false;
				for (Phrase ip : interest.getPhrases()) {
					if (ip.containPhrase(p)) {
						flag = true;
						break;
					}
				}

				if (flag)
					continue;

				if (tweet.containsPhrase(p)) {
					double prel = (double) relevantPatterns.get(pattern)
							/ (double) phrase.getStatistics()
									.getTotalRelevantTweetCount();
					count++;
					// logger.info(phrase.getText() + "\t~\t" + pattern + "\t"
					// + prel);
					if (prel > maxRel || maxRel == 0)
						maxRel = prel;
					else
						rel += prel;

					if (rel > 1)
						return 1;
				}
			}
		}

		rel = Math.abs(maxRel
				+ (count > 1 ? (1 - maxRel) * (rel / (count - 1)) : 0));

		if (Double.isNaN(rel))
			rel = 0;

		return rel > 1 ? 1 : rel;
	}

	private static double getUserRelevance(Tweet tweet, Interest interest) {
		UserStatistics userStatistics = interest.getUserStatistics(tweet
				.getUserID());
		if (userStatistics != null) {
			double totalAvgRelevance = userStatistics.getAvgRelevance();
			if (!Double.isNaN(totalAvgRelevance)) {
				double r = totalAvgRelevance > 1 ? 1 : totalAvgRelevance;
				return r;
			}
		}

		return 0;
	}

	public static double getSatisfaction(Tweet tweet, Interest interest,
			Phrase phrase) {
		double satisfaction = 0;
		int matchCount = 0;

		HashSet<String> phraseTerms = phrase.getTerms();
		for (String pterm : phraseTerms) {
			pterm = pterm.trim();
			for (String tterm : tweet.getTerms()) {
				tterm = tterm.trim();
				if (pterm.equals(tterm)) {
					double freq = interest.getPhraseTermFreq(pterm);
					satisfaction += freq;
					matchCount++;
					break;
				}
			}
		}

		if (phraseTerms.size() == matchCount)
			return 1;

		if (phraseTerms.size() > 0) {
			double s = satisfaction / (double) (phraseTerms.size());
			return s;
		}

		return 0;
	}

}
