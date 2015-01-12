package ta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.mongodb.morphia.annotations.Transient;

import twitter4j.HashtagEntity;
import twitter4j.UserMentionEntity;
import util.SetUtil;
import conf.ConfigMgr;
import conf.Tweet;

public class WindowStatistics {
	private static final double irrelevanceThreshold = .1;
	@Transient
	public volatile boolean done = false;
	@Transient
	private volatile static int minNumberOfTweets = 10;
	private volatile Map<String, Integer> relevantPatterns;
	private volatile Map<String, Integer> irrelevantPatterns;
	private volatile Map<String, Integer> relevantHashtags;
	private volatile Map<String, Integer> irrelevantHashtags;
	private volatile Map<String, Integer> relevantMentions;
	private volatile Map<String, Integer> irrelevantMentions;

	private volatile int relevantTweetCount;
	private volatile int irrelevantTweetCount;

	private volatile double avgRelevance;
	private volatile double maxRelevance;
	private volatile double minRelevance;
	@Transient
	private volatile double newPhraseMinSup;
	@Transient
	private volatile Integer newPhraseMaxLength;
	private volatile long duration;

	public WindowStatistics() {
		newPhraseMinSup = Double.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionNewPhraseMinSup"));
		newPhraseMaxLength = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionNewPhraseMaxLength"));
		relevantTweetCount = 0;
		irrelevantTweetCount = 0;
		avgRelevance = 0;
		maxRelevance = 0;
		minRelevance = 0;
		duration = 0;
		relevantPatterns = new ConcurrentHashMap<String, Integer>();
		irrelevantPatterns = new ConcurrentHashMap<String, Integer>();
		relevantHashtags = new ConcurrentHashMap<String, Integer>();
		irrelevantHashtags = new ConcurrentHashMap<String, Integer>();
		relevantMentions = new ConcurrentHashMap<String, Integer>();
		irrelevantMentions = new ConcurrentHashMap<String, Integer>();
	}

	public void addTweet(Tweet tweet) {
		double relevanceThreshold = Acquisition.getInterest()
				.getTweetRelevanceThreshold();

		updateRelevanceStatistics(tweet);

		if (tweet.getRelevance() < relevanceThreshold
				&& tweet.getRelevance() > irrelevanceThreshold)
			return;

		addHashtags(tweet);
		addMentions(tweet);
		extractPatterns(tweet);

		if (relevantPatterns.size() > 1000000)
			cleanRelevantPatterns();
		if (irrelevantPatterns.size() > 1000000)
			cleanIrrelevantPatterns();
	}

	private synchronized void updateRelevanceStatistics(Tweet tweet) {
		double relevanceThreshold = Acquisition.getInterest()
				.getTweetRelevanceThreshold();
		double r = tweet.getRelevance();

		if (r > maxRelevance)
			maxRelevance = r;
		if (r <= minRelevance || minRelevance == 0)
			minRelevance = r;

		if (r < relevanceThreshold)
			irrelevantTweetCount++;
		else
			relevantTweetCount++;

		avgRelevance = ((double) (getTotalTweetCount() - 1) * avgRelevance + r)
				/ (double) getTotalTweetCount();
	}

	private synchronized void extractPatterns(Tweet tweet) {
		HashSet<String> tweetTermSet = new HashSet<String>(tweet.getTerms());
		Set<Set<String>> patterns = SetUtil.powerSet(tweetTermSet,
				newPhraseMaxLength);

		for (Set<String> p : patterns) {
			if (p.size() <= 0)
				continue;

			String pattern = "";
			String phrase = "";
			for (String t : p) {
				pattern += t + ",";
				phrase += t + " ";
			}
			pattern = pattern.substring(0, pattern.length() - 1).trim();
			phrase = phrase.trim();

			if (Acquisition.getInterest().hasPhrase(phrase)
					|| Acquisition.getInterest().coverPhrase(phrase))
				continue;

			updatePatterns(tweet, pattern);
		}
	}

	private synchronized void updatePatterns(Tweet tweet, String pattern) {
		double relevanceThreshold = Acquisition.getInterest()
				.getTweetRelevanceThreshold();

		Map<String, Integer> patterns;
		patterns = tweet.getRelevance() > relevanceThreshold ? getRelevantPatterns()
				: getIrrelevantPatterns();

		if (patterns.containsKey(pattern))
			patterns.put(pattern, patterns.get(pattern) + 1);
		else
			patterns.put(pattern, 1);
	}

	private synchronized void addHashtags(Tweet tweet) {
		double relevanceThreshold = Acquisition.getInterest()
				.getTweetRelevanceThreshold();
		Map<String, Integer> hashtags;
		hashtags = tweet.getRelevance() > relevanceThreshold ? relevantHashtags
				: irrelevantHashtags;

		for (HashtagEntity x : tweet.getStatus().getHashtagEntities()) {
			String hashtag = x.getText().toLowerCase();
			hashtags.put(hashtag,
					hashtags.containsKey(hashtag) ? hashtags.get(hashtag) + 1
							: 1);
		}
	}

	private synchronized void addMentions(Tweet tweet) {
		double relevanceThreshold = Acquisition.getInterest()
				.getTweetRelevanceThreshold();
		Map<String, Integer> mentions;
		mentions = tweet.getRelevance() > relevanceThreshold ? relevantMentions
				: irrelevantMentions;

		for (UserMentionEntity y : tweet.getStatus().getUserMentionEntities()) {
			String mention = y.getText().toLowerCase();
			mentions.put(mention,
					mentions.containsKey(mention) ? mentions.get(mention) + 1
							: 1);
		}
	}

	public void finalize(TotalStatistics totalStatistics, long d) {
		cleanRelevantPatterns(totalStatistics);
		cleanIrrelevantPatterns(totalStatistics);

		clean(relevantHashtags, 2);
		for (String ht : relevantHashtags.keySet())
			totalStatistics.addRelevantHashtag(ht, relevantHashtags.get(ht));

		clean(irrelevantHashtags, 2);
		for (String ht : irrelevantHashtags.keySet())
			totalStatistics
					.addIrrelevantHashtag(ht, irrelevantHashtags.get(ht));

		clean(relevantMentions, 2);
		clean(irrelevantMentions, 2);

		done = true;
		setDuration(d);
	}

	private void cleanIrrelevantPatterns(TotalStatistics totalStatistics) {
		Map<String, Integer> tifp = new HashMap<String, Integer>();
		if (getIrrelevantTweetCount() > minNumberOfTweets) {
			for (Entry<String, Integer> y : irrelevantPatterns.entrySet()) {
				double sup = (double) y.getValue() / irrelevantTweetCount;
				if (sup > newPhraseMinSup && y.getKey().length() > 2) {
					tifp.put(y.getKey(), y.getValue());
					totalStatistics.addIrrelevantPattern(y.getKey(),
							y.getValue());
				}
			}
		}

		irrelevantPatterns = tifp;
	}

	private void cleanIrrelevantPatterns() {
		Map<String, Integer> tifp = new HashMap<String, Integer>();
		if (getIrrelevantTweetCount() > minNumberOfTweets) {
			for (Entry<String, Integer> y : irrelevantPatterns.entrySet()) {
				double sup = (double) y.getValue() / irrelevantTweetCount;
				if (sup > newPhraseMinSup && y.getKey().length() > 2) {
					tifp.put(y.getKey(), y.getValue());
				}
			}
		}

		irrelevantPatterns = tifp;
	}

	private void cleanRelevantPatterns(TotalStatistics totalStatistics) {
		Map<String, Integer> trfp = new HashMap<String, Integer>();
		if (getRelevantTweetCount() > minNumberOfTweets) {
			for (Entry<String, Integer> y : relevantPatterns.entrySet()) {
				double sup = (double) y.getValue() / relevantTweetCount;
				if (sup > newPhraseMinSup && y.getKey().length() > 2) {
					trfp.put(y.getKey(), y.getValue());
					totalStatistics
							.addRelevantPattern(y.getKey(), y.getValue());
				}
			}
		}
		relevantPatterns = trfp;
	}

	private void cleanRelevantPatterns() {
		Map<String, Integer> trfp = new HashMap<String, Integer>();
		if (getRelevantTweetCount() > minNumberOfTweets) {
			for (Entry<String, Integer> y : relevantPatterns.entrySet()) {
				double sup = (double) y.getValue() / relevantTweetCount;
				if (sup > newPhraseMinSup && y.getKey().length() > 2) {
					trfp.put(y.getKey(), y.getValue());
				}
			}
		}
		relevantPatterns = trfp;
	}

	private void clean(Map<String, Integer> input, int th) {
		List<String> toBeRemoved = new ArrayList<String>();
		for (String ht : input.keySet())
			if (input.get(ht) < th)
				toBeRemoved.add(ht);

		for (String tbrht : toBeRemoved)
			input.remove(tbrht);
	}

	public double getMaxRelevance() {
		return maxRelevance;
	}

	public double getMinRelevance() {
		return minRelevance;
	}

	public int getRelevantTweetCount() {
		return relevantTweetCount;
	}

	public int getIrrelevantTweetCount() {
		return irrelevantTweetCount;
	}

	public double getAvgRelevance() {
		return avgRelevance;
	}

	public int getTotalTweetCount() {
		return getRelevantTweetCount() + getIrrelevantTweetCount();
	}

	public void setRelevantTweetCount(int relevantTweetCount) {
		this.relevantTweetCount = relevantTweetCount;
	}

	public void setIrrelevantTweetCount(int irrelevantTweetCount) {
		this.irrelevantTweetCount = irrelevantTweetCount;
	}

	public void setAvgRelevance(double avgRelevance) {
		this.avgRelevance = avgRelevance;
	}

	public void setMaxRelevance(double maxRelevance) {
		this.maxRelevance = maxRelevance;
	}

	public void setMinRelevance(double minRelevance) {
		this.minRelevance = minRelevance;
	}

	public Map<String, Integer> getRelevantPatterns() {
		return relevantPatterns;
	}

	public void setRelevantPatterns(
			Map<String, Integer> frequentRelevantPatterns) {
		this.relevantPatterns = frequentRelevantPatterns;
	}

	public Map<String, Integer> getIrrelevantPatterns() {
		return irrelevantPatterns;
	}

	public void setIrrelevantPatterns(
			Map<String, Integer> frequentIrrelevantPatterns) {
		this.irrelevantPatterns = frequentIrrelevantPatterns;
	}

	public Map<String, Integer> getRelevantHashtags() {
		return relevantHashtags;
	}

	public Map<String, Integer> getIrrelevantHashtags() {
		return irrelevantHashtags;
	}

	public Map<String, Integer> getRelevantMentions() {
		return relevantMentions;
	}

	public Map<String, Integer> getIrrelevantMentions() {
		return irrelevantMentions;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}
}
