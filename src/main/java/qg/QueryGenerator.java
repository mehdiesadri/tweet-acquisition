package qg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ta.Acquisition;
import conf.Interest;
import conf.Phrase;
import conf.Query;

public class QueryGenerator {
	static final Logger logger = LogManager.getLogger(QueryGenerator.class
			.getName());

	private static QueryGenerator instance = null;

	public static synchronized QueryGenerator getInstance() {
		if (instance == null)
			instance = new QueryGenerator();

		return instance;
	}

	public static Query generate(Interest interest) {
		Query query = new Query();
		List<Phrase> phrases = new ArrayList<Phrase>();

		int exploreSize = (int) (Acquisition.eefraction * Acquisition.phraseLimit);
		int exploitSize = Acquisition.phraseLimit - exploreSize;

		int interestTotalTweetCount = interest.getStatistics()
				.getRelevantTweetCount()
				+ interest.getStatistics().getIrrelevantTweetCount();
		if (interestTotalTweetCount == 0)
			exploreSize = Acquisition.phraseLimit;

		for (Phrase phrase : interest.getPhrases())
			phrases.add(phrase);

		logger.info("Total Interest's Phrase Count: " + phrases.size());
		int phrasesToExploitCount = 0;
		if (interestTotalTweetCount > 0) {
			Collection<? extends Phrase> phrasesToExploit = selectPhrasesToExploit(
					phrases, exploitSize);
			for (Phrase phrase : phrasesToExploit) {
				if (phrase.getStatistics().getStatCount() > 0
						&& !query.getPhrases().containsKey(phrase.getText())) {
					phrase.getStatistics().addNewStat();
					phrase.setLastUpdateTime(System.currentTimeMillis());
					addPhraseToQuery(query, phrase);
					phrases.remove(phrase);
					phrasesToExploitCount++;
				}
			}

			logger.info("Number of phrases to exploit: "
					+ phrasesToExploitCount);
		}

		int phrasesToExploreCount = 0;
		Collection<? extends Phrase> phrasesToExplore = selectPhrasesToExplore(
				phrases, exploreSize);
		for (Phrase phrase : phrasesToExplore) {
			phrase.getStatistics().addNewStat();
			phrase.setLastUpdateTime(System.currentTimeMillis());
			addPhraseToQuery(query, phrase);
			phrasesToExploreCount++;
		}

		logger.info("Number of phrases to explore: " + phrasesToExploreCount);
		return query;
	}

	private static void addPhraseToQuery(Query query, Phrase phrase) {
		List<String> toBeRemoved = new ArrayList<String>();
		for (String pstr : query.getPhrases().keySet()) {
			Phrase queryPhrase = query.getPhrases().get(pstr);
			if (phrase.coverPhrase(queryPhrase.getText()))
				toBeRemoved.add(pstr);
			if (query.getPhrases().get(pstr).coverPhrase(phrase.getText()))
				return;
		}

		for (String pstr : toBeRemoved)
			query.getPhrases().remove(pstr);

		query.addPhrase(phrase);
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
			if (i >= phrases.size())
				break;
			Phrase phrase = phrases.get(i);
			addPhraseToList(ps, phrase);
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
					c = Integer.valueOf(o2.getTerms().size()).compareTo(
							o1.getTerms().size());
				if (c == 0)
					c = Long.valueOf(o2.getLastUpdateTime()).compareTo(
							o1.getLastUpdateTime());
				return c;
			}
		});

		for (int i = 0; i < exploreSize; i++) {
			if (i >= phrases.size())
				break;
			Phrase phrase = phrases.get(i);
			addPhraseToList(ps, phrase);
		}

		return ps;
	}

	private static void addPhraseToList(List<Phrase> ps, Phrase phrase) {
		List<Phrase> covering = new ArrayList<Phrase>();
		for (Phrase p : ps) {
			if (phrase.coverPhrase(p.getText())
					|| p.coverPhrase(phrase.getText())) {
				covering.add(p);
			}
		}

		int count = 0;
		for (Phrase p : covering) {
			if (phrase.coverPhrase(p.getText())) {
				ps.remove(p);
				count++;
			}
		}

		if (count <= covering.size())
			ps.add(phrase);
	}

}