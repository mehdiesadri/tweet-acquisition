package rc;

import java.util.Collection;

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

		String[] phraseTerms = phrase.getTerms();
		for (String pterm : phraseTerms) {
			pterm = pterm.trim();
			satisfaction += interest.getPhraseTermFreq(pterm);
		}

		if (phraseTerms.length > 0)
			return satisfaction / ((double) phraseTerms.length);

		return 0;
	}

	public synchronized static double getRelevance(Tweet tweet,
			Interest interest) {
		double maxSatisfaction = 0;
		double phraseRel = 0;

		Collection<Phrase> phrases = interest.getPhrases();
		synchronized (phrases) {
			for (Phrase phrase : phrases) {
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

		double cluRel = getCluRelevance(tweet, interest);
		double userRel = getUserRelevance(tweet, interest);

		double relevance = 0;
		if (userRel > 0 && cluRel > 0) {
			relevance = .3 * userRel + .4 * cluRel + .3 * phraseRel;
		} else if (userRel > 0) {
			relevance = .4 * userRel + .6 * phraseRel;
		} else if (cluRel > 0) {
			relevance = .5 * cluRel + .5 * phraseRel;
		} else {
			relevance = phraseRel;
		}

		relevance = relevance > 1 ? 1 : relevance;
		return relevance;
	}

	private static double getCluRelevance(Tweet tweet, Interest interest) {
		// TODO Auto-generated method stub
		return 0;
	}

	private static double getUserRelevance(Tweet tweet, Interest interest) {
		UserStatistics userStatistics = interest.getUserStatistics(tweet
				.getUserID());
		if (userStatistics != null) {
			double totalAvgRelevance = userStatistics.getAvgRelevance();
			if (!Double.isNaN(totalAvgRelevance))
				return totalAvgRelevance;
		}
		return 0;
	}

	public static double getSatisfaction(Tweet tweet, Interest interest,
			Phrase phrase) {
		double satisfaction = 0;
		int matchCount = 0;

		String[] phraseTerms = phrase.getTerms();
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

		if (phraseTerms.length == matchCount)
			return 1;

		if (phraseTerms.length > 0)
			return satisfaction / (double) (phraseTerms.length);
		return 0;
	}

}
