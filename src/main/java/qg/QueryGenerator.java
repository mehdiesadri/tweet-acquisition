package qg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import conf.ConfigMgr;
import conf.Interest;
import conf.Location;
import conf.Phrase;
import conf.Query;

public class QueryGenerator {
	static final Logger logger = LogManager.getLogger(QueryGenerator.class
			.getName());

	private static double eefraction;
	private static int phraseLimit;
	private static int locationLimit;
	private static int userLimit;

	private static QueryGenerator instance = null;

	public static synchronized QueryGenerator getInstance() {
		if (instance == null)
			instance = new QueryGenerator();

		return instance;
	}

	public QueryGenerator() {
		eefraction = Double.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionEEFraction"));
		phraseLimit = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionPhraseLimit"));
		locationLimit = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionLocationLimit"));
		userLimit = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionUserLimit"));
	}

	public static Query generate(Interest interest) {
		Query query = new Query();
		List<Phrase> phrases = new ArrayList<Phrase>();

		int exploreSize = (int) (eefraction * phraseLimit);
		int exploitSize = phraseLimit - exploreSize;

		if (interest.getStatistics().getTotalTweetCount() == 0)
			exploreSize = phraseLimit;

		for (Phrase phrase : interest.getPhrases())
			phrases.add(phrase);

		logger.info("Total Interest's Phrase Count: " + phrases.size());

		Collection<? extends Phrase> phrasesToExplore = selectPhrasesToExplore(
				phrases, exploreSize);
		for (Phrase phrase : phrasesToExplore) {
			phrase.getStatistics().addStat();
			phrase.setLastUpdateTime(System.currentTimeMillis());
			query.addPhrase(phrase);
		}

		logger.info("Number of phrases to explore: " + phrasesToExplore.size());

		if (interest.getStatistics().getTotalTweetCount() == 0)
			return query;

		Collection<? extends Phrase> phrasesToExploit = selectPhrasesToExploit(
				phrases, exploitSize);
		for (Phrase phrase : phrasesToExploit)
			if (!query.getPhrases().containsKey(phrase.getText())) {
				phrase.getStatistics().addStat();
				phrase.setLastUpdateTime(System.currentTimeMillis());
				query.addPhrase(phrase);
			}

		logger.info("Number of phrases to exploit: " + phrasesToExploit.size());

		return query;
	}

	private static Collection<? extends Phrase> selectPhrasesToExploit(
			List<Phrase> phrases, int exploitSize) {
		List<Phrase> ps = new ArrayList<Phrase>();
		Collections.sort(phrases, new Comparator<Phrase>() {
			public int compare(Phrase o1, Phrase o2) {
				int c = Double.valueOf(o2.computeReward()).compareTo(
						o1.computeReward());
				return c;
			}
		});

		for (int i = 0; i < exploitSize; i++) {
			if (i < phrases.size())
				ps.add(phrases.get(i));
		}

		return ps;
	}

	private static Collection<? extends Phrase> selectPhrasesToExplore(
			List<Phrase> phrases, int exploreSize) {
		List<Phrase> ps = new ArrayList<Phrase>();
		Collections.sort(phrases, new Comparator<Phrase>() {
			public int compare(Phrase o1, Phrase o2) {
				int c;
				c = Double.valueOf(o2.getWeight()).compareTo(o1.getWeight());
				if (c == 0)
					c = Integer.valueOf(o2.getTerms().length).compareTo(
							o1.getTerms().length);
				if (c == 0)
					c = Long.valueOf(o2.getLastUpdateTime()).compareTo(
							o1.getLastUpdateTime());
				return c;
			}
		});

		for (int i = 0; i < exploreSize; i++) {
			if (i < phrases.size())
				ps.add(phrases.get(i));
		}

		return ps;
	}

	private static boolean covered(String phrase1, String phrase2) {
		List<String> words1 = new ArrayList<String>();
		List<String> words2 = new ArrayList<String>();

		for (String word : phrase1.split(" "))
			words1.add(word);

		for (String word : phrase2.split(" "))
			words2.add(word);

		if (words1.size() <= words2.size()) {
			for (String word : words1) {
				if (!words2.contains(word))
					return false;
			}
			return true;
		} else {
			for (String word : words2) {
				if (!words1.contains(word))
					return false;
			}
			return true;
		}
	}
}