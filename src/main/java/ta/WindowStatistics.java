package ta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import twitter4j.HashtagEntity;
import twitter4j.UserMentionEntity;
import util.SetUtil;
import conf.ConfigMgr;
import conf.Tweet;

public class WindowStatistics {
	public volatile boolean done = false;

	private static final int minNumberOfTweets = 10;
	private Map<String, Integer> relevantPatterns;
	private Map<String, Integer> irrelevantPatterns;
	private Map<String, Integer> relevantHashtags;
	private Map<String, Integer> irrelevantHashtags;
	private Map<String, Integer> relevantMentions;
	private Map<String, Integer> irrelevantMentions;

	private int relevantTweetCount;
	private int irrelevantTweetCount;
	private double avgRelevance;
	private double maxRelevance;
	private double minRelevance;
	private double newPhraseMinSup;
	private Integer newPhraseMaxLength;

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

		relevantPatterns = new ConcurrentHashMap<String, Integer>();
		irrelevantPatterns = new ConcurrentHashMap<String, Integer>();

		relevantHashtags = new ConcurrentHashMap<String, Integer>();
		irrelevantHashtags = new ConcurrentHashMap<String, Integer>();
		relevantMentions = new ConcurrentHashMap<String, Integer>();
		irrelevantMentions = new ConcurrentHashMap<String, Integer>();
	}

	public void addTweet(Tweet tweet) {
		double r = tweet.getRelevance();

		if (r > maxRelevance)
			maxRelevance = r;
		if (r < minRelevance || minRelevance == 0)
			minRelevance = r;
		if (r >= Acquisition.getInterest().getTweetRelevanceThreshold())
			relevantTweetCount++;
		else
			irrelevantTweetCount++;

		avgRelevance = ((double) (getTotalTweetCount() - 1) * avgRelevance + r)
				/ (double) getTotalTweetCount();

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

			if (r > Acquisition.getInterest().getTweetRelevanceThreshold()) {
				addHashtags(tweet, relevantHashtags);
				addMentions(tweet, relevantMentions);

				if (getFrequentRelevantPatterns().containsKey(pattern)) {
					getFrequentRelevantPatterns().put(pattern,
							getFrequentRelevantPatterns().get(pattern) + 1);
				} else {
					getFrequentRelevantPatterns().put(pattern, 1);
				}
			} else {
				addHashtags(tweet, irrelevantHashtags);
				addMentions(tweet, irrelevantMentions);

				if (getFrequentIrrelevantPatterns().containsKey(pattern)) {
					getFrequentIrrelevantPatterns().put(pattern,
							getFrequentIrrelevantPatterns().get(pattern) + 1);
				} else {
					getFrequentIrrelevantPatterns().put(pattern, 1);
				}

			}
		}
	}

	private void addHashtags(Tweet tweet, Map<String, Integer> hashtags) {
		for (HashtagEntity x : tweet.getStatus().getHashtagEntities()) {
			String hashtag = x.getText().toLowerCase();
			hashtags.put(hashtag,
					hashtags.containsKey(hashtag) ? hashtags.get(hashtag) + 1
							: 1);
		}
	}

	private void addMentions(Tweet tweet, Map<String, Integer> mentions) {
		for (UserMentionEntity y : tweet.getStatus().getUserMentionEntities()) {
			String mention = y.getText().toLowerCase();
			mentions.put(mention,
					mentions.containsKey(mention) ? mentions.get(mention) + 1
							: 1);
		}
	}

	public void finalize(TotalStatistics totalStatistics) {
		Map<String, Integer> trfp = new HashMap<String, Integer>();
		if (getRelevantTweetCount() > minNumberOfTweets) {
			for (Entry<String, Integer> y : relevantPatterns.entrySet()) {
				if (((double) y.getValue() / relevantTweetCount) > newPhraseMinSup
						&& y.getKey().length() > 2) {
					trfp.put(y.getKey(), y.getValue());
					totalStatistics.addRelevant(y.getKey(), y.getValue());
				}
			}
		}
		relevantPatterns = trfp;

		Map<String, Integer> tifp = new HashMap<String, Integer>();
		if (getIrrelevantTweetCount() > minNumberOfTweets) {
			for (Entry<String, Integer> y : irrelevantPatterns.entrySet()) {
				if (((double) y.getValue() / irrelevantTweetCount) > newPhraseMinSup
						&& y.getKey().length() > 2) {
					tifp.put(y.getKey(), y.getValue());
					totalStatistics.addIrrelevant(y.getKey(), y.getValue());
				}
			}
		}
		irrelevantPatterns = tifp;
		done = true;
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

	public Map<String, Integer> getFrequentRelevantPatterns() {
		return relevantPatterns;
	}

	public void setFrequentRelevantPatterns(
			Map<String, Integer> frequentRelevantPatterns) {
		this.relevantPatterns = frequentRelevantPatterns;
	}

	public Map<String, Integer> getFrequentIrrelevantPatterns() {
		return irrelevantPatterns;
	}

	public void setFrequentIrrelevantPatterns(
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
}
