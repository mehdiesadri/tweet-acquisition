package ta;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;

import twitter4j.HashtagEntity;
import twitter4j.UserMentionEntity;
import txt.TextNormalizer;
import util.MapUtil;
import util.SetUtil;
import conf.Tweet;

@Embedded("statistics")
public class WindowStatistics extends Statistics {
	private volatile Map<String, Integer> relevantPatterns;
	private volatile Map<String, Integer> irrelevantPatterns;
	private volatile Map<String, Integer> relevantHashtags;
	private volatile Map<String, Integer> irrelevantHashtags;

	public volatile AtomicInteger addedPhraseCount;
	public volatile AtomicInteger removedPhraseCount;
	public volatile AtomicInteger generalizedPhraseCount;
	public volatile AtomicInteger specializedPhraseCount;

	@Transient
	private volatile boolean done;

	private volatile Map<String, Integer> relevantMentions;
	private volatile Map<String, Integer> irrelevantMentions;

	@Transient
	private volatile long duration;

	public WindowStatistics(TotalStatistics pstat) {
		addedPhraseCount = new AtomicInteger(0);
		removedPhraseCount = new AtomicInteger(0);
		generalizedPhraseCount = new AtomicInteger(0);
		specializedPhraseCount = new AtomicInteger(0);

		duration = 0;
		done = false;

		relevantPatterns = new ConcurrentHashMap<String, Integer>();
		irrelevantPatterns = new ConcurrentHashMap<String, Integer>();
		relevantHashtags = new ConcurrentHashMap<String, Integer>();
		irrelevantHashtags = new ConcurrentHashMap<String, Integer>();
		relevantMentions = new ConcurrentHashMap<String, Integer>();
		irrelevantMentions = new ConcurrentHashMap<String, Integer>();
	}

	@Override
	public void addTweet(Tweet tweet) {
		super.addTweet(tweet);
		if (tweet.getRelevance() < relevanceThreshold
				&& tweet.getRelevance() > irrelevanceThreshold)
			return;

		addHashtags(tweet);
		addMentions(tweet);
		extractPatterns(tweet);

		if (relevantPatterns.size() > Acquisition.maxNumberOfPatterns)
			clean(relevantPatterns, relevantTweetCount.get());
		if (irrelevantPatterns.size() > Acquisition.maxNumberOfPatterns)
			clean(irrelevantPatterns, irrelevantTweetCount.get());
	}

	private synchronized void extractPatterns(Tweet tweet) {
		HashSet<String> tweetTermSet = tweet.getTerms();
		Set<Set<String>> patterns = SetUtil.powerSet(tweetTermSet,
				Acquisition.newPhraseMaxLength);

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
		Map<String, Integer> patterns;
		patterns = tweet.getRelevance() > relevanceThreshold ? getRelevantPatterns()
				: getIrrelevantPatterns();

		synchronized (patterns) {
			if (patterns.containsKey(pattern))
				patterns.put(pattern, patterns.get(pattern) + 1);
			else
				patterns.put(pattern, 1);
		}
	}

	private synchronized void addHashtags(Tweet tweet) {
		Map<String, Integer> hashtags = tweet.getRelevance() > relevanceThreshold ? relevantHashtags
				: irrelevantHashtags;

		synchronized (hashtags) {
			for (HashtagEntity x : tweet.getStatus().getHashtagEntities()) {
				String hashtag = x.getText().toLowerCase();
				List<String> hparts = TextNormalizer.normalize(hashtag);
				for (String hp : hparts) {
					if (hp.length() > 2)
						hashtags.put(
								hp,
								hashtags.containsKey(hashtag) ? hashtags
										.get(hashtag) + 1 : 1);
				}
			}
		}
	}

	private synchronized void addMentions(Tweet tweet) {
		Map<String, Integer> mentions = tweet.getRelevance() > relevanceThreshold ? relevantMentions
				: irrelevantMentions;

		synchronized (mentions) {
			for (UserMentionEntity y : tweet.getStatus()
					.getUserMentionEntities()) {
				String mention = y.getText().toLowerCase();
				mentions.put(
						mention,
						mentions.containsKey(mention) ? mentions.get(mention) + 1
								: 1);
			}
		}
	}

	public void finalize(TotalStatistics totalStatistics, long d) {
		clean(relevantPatterns, relevantTweetCount.get());
		clean(irrelevantPatterns, irrelevantTweetCount.get());
		clean(relevantHashtags, relevantTweetCount.get());
		clean(irrelevantHashtags, irrelevantTweetCount.get());
		clean(relevantMentions, relevantTweetCount.get());
		clean(irrelevantMentions, irrelevantTweetCount.get());

		for (String k : relevantPatterns.keySet())
			totalStatistics.addRelevantPattern(k, relevantPatterns.get(k));

		for (String k : irrelevantPatterns.keySet())
			totalStatistics.addIrrelevantPattern(k, irrelevantPatterns.get(k));

		for (String ht : relevantHashtags.keySet())
			totalStatistics.addRelevantHashtag(ht, relevantHashtags.get(ht));

		for (String ht : irrelevantHashtags.keySet())
			totalStatistics
					.addIrrelevantHashtag(ht, irrelevantHashtags.get(ht));

		done = true;
		setDuration(d);
	}

	private void clean(Map<String, Integer> input, int totalCount) {
		synchronized (input) {
			List<Entry<String, Integer>> sortedEntryList = MapUtil
					.sortByValue(input);
			input.clear();

			for (int i = 0; i < Math.min(sortedEntryList.size(),
					Acquisition.maxNumberOfPatterns); i++) {
				String key = (String) sortedEntryList.get(i).getKey();
				int value = (int) sortedEntryList.get(i).getValue();
				double freq = (double) value / (double) totalCount;
				if (freq > Acquisition.newPhraseMinSup)
					input.put(key, value);
			}
		}
	}

	public double getMaxRelevance() {
		return maxRelevance;
	}

	public double getMinRelevance() {
		return minRelevance;
	}

	public double getAvgRelevance() {
		return avgRelevance;
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

	public boolean isDone() {
		return done;
	}
}
