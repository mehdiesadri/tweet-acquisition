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
	private Map<String, Integer> hashtags;
	private Map<String, Integer> mentions;

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

		hashtags = new ConcurrentHashMap<String, Integer>();
		mentions = new ConcurrentHashMap<String, Integer>();
	}

	public void addTweet(Tweet tweet) {
		double r = tweet.getRelevance();

		for (HashtagEntity x : tweet.getStatus().getHashtagEntities()) {
			String hashtag = x.getText().toLowerCase();
			getHashtags().put(
					hashtag,
					getHashtags().containsKey(hashtag) ? getHashtags().get(
							hashtag) + 1 : 1);
		}

		for (UserMentionEntity y : tweet.getStatus().getUserMentionEntities()) {
			String mention = y.getText().toLowerCase();
			getMentions().put(
					mention,
					getMentions().containsKey(mention) ? getMentions().get(
							mention) + 1 : 1);
		}

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

			if (tweet.getRelevance() > Acquisition.getInterest()
					.getTweetRelevanceThreshold()) {
				if (getFrequentRelevantPatterns().containsKey(pattern)) {
					getFrequentRelevantPatterns().put(pattern,
							getFrequentRelevantPatterns().get(pattern) + 1);
				} else {
					getFrequentRelevantPatterns().put(pattern, 1);
				}
			} else {
				if (getFrequentIrrelevantPatterns().containsKey(pattern)) {
					getFrequentIrrelevantPatterns().put(pattern,
							getFrequentIrrelevantPatterns().get(pattern) + 1);
				} else {
					getFrequentIrrelevantPatterns().put(pattern, 1);
				}

			}
		}
	}

	public void finalize() {
		Map<String, Integer> trfp = new HashMap<String, Integer>();
		if (getRelevantTweetCount() > minNumberOfTweets) {
			for (Entry<String, Integer> y : relevantPatterns.entrySet()) {
				if (((double) y.getValue() / relevantTweetCount) > newPhraseMinSup
						&& y.getKey().length() > 2)
					trfp.put(y.getKey(), y.getValue());
			}
		}
		relevantPatterns = trfp;

		Map<String, Integer> tifp = new HashMap<String, Integer>();
		if (getIrrelevantTweetCount() > minNumberOfTweets) {
			for (Entry<String, Integer> y : irrelevantPatterns.entrySet()) {
				if (((double) y.getValue() / irrelevantTweetCount) > newPhraseMinSup
						&& y.getKey().length() > 2)
					tifp.put(y.getKey(), y.getValue());
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

	public Map<String, Integer> getHashtags() {
		return hashtags;
	}

	public Map<String, Integer> getMentions() {
		return mentions;
	}

}
