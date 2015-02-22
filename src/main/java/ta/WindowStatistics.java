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
public class WindowStatistics {
	@Transient
	private volatile boolean done;

	private volatile Map<String, Integer> relevantPatterns;
	private volatile Map<String, Integer> irrelevantPatterns;
	private volatile Map<String, Integer> relevantHashtags;
	private volatile Map<String, Integer> irrelevantHashtags;
	private volatile Map<String, Integer> relevantMentions;
	private volatile Map<String, Integer> irrelevantMentions;

	private volatile AtomicInteger totalTweetCount;
	private volatile AtomicInteger relevantTweetCount;
	private volatile AtomicInteger irrelevantTweetCount;
	private volatile AtomicInteger neutralTweetCount;
	private volatile AtomicInteger deltaTweetCount;

	private volatile double avgRelevance;
	private volatile double maxRelevance;
	private volatile double minRelevance;

	@Transient
	private volatile long duration;
	@Transient
	private double relevanceThreshold;
	@Transient
	private double irrelevanceThreshold;
	@Transient
	private TotalStatistics parent;

	public WindowStatistics(TotalStatistics pstat) {
		parent = pstat;
		relevanceThreshold = Acquisition.getInterest()
				.getTweetRelevanceThreshold();
		irrelevanceThreshold = Acquisition.getInterest()
				.getTweetIrrelevanceThreshold();

		totalTweetCount = new AtomicInteger(0);
		relevantTweetCount = new AtomicInteger(0);
		irrelevantTweetCount = new AtomicInteger(0);
		neutralTweetCount = new AtomicInteger(0);
		deltaTweetCount = new AtomicInteger(0);

		avgRelevance = 0;
		maxRelevance = 0;
		minRelevance = 0;

		duration = 0;
		done = false;

		relevantPatterns = new ConcurrentHashMap<String, Integer>();
		irrelevantPatterns = new ConcurrentHashMap<String, Integer>();
		relevantHashtags = new ConcurrentHashMap<String, Integer>();
		irrelevantHashtags = new ConcurrentHashMap<String, Integer>();
		relevantMentions = new ConcurrentHashMap<String, Integer>();
		irrelevantMentions = new ConcurrentHashMap<String, Integer>();
	}

	public void addTweet(Tweet tweet) {
		totalTweetCount.incrementAndGet();

		if (tweet.getRelevance() < 0)
			return;

		updateRelevanceStatistics(tweet);

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

	private synchronized void updateRelevanceStatistics(Tweet tweet) {
		double r = tweet.getRelevance();

		synchronized (this) {
			if (r > relevanceThreshold)
				relevantTweetCount.incrementAndGet();
			else if (r < irrelevanceThreshold)
				irrelevantTweetCount.incrementAndGet();
			else
				neutralTweetCount.incrementAndGet();

			if (r > maxRelevance)
				maxRelevance = r;

			if (r <= minRelevance || minRelevance == 0)
				minRelevance = r;

			avgRelevance = ((double) (getTotalTweetCount() - 1) * avgRelevance + r)
					/ (double) getTotalTweetCount();
		}
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
		parent.addStatInfo(this);
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

	public int getTotalTweetCount() {
		return totalTweetCount.get();
	}

	public int getRelevantTweetCount() {
		return relevantTweetCount.get();
	}

	public int getIrrelevantTweetCount() {
		return irrelevantTweetCount.get();
	}

	public void setRelevantTweetCount(int rtc) {
		this.relevantTweetCount.set(rtc);
	}

	public void setIrrelevantTweetCount(int itc) {
		this.irrelevantTweetCount.set(itc);
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

	public int getDeltaTweetCount() {
		return deltaTweetCount.get();
	}

	public void setDeltaTweetCount(int dtc) {
		this.deltaTweetCount.set(dtc);
	}

	public void incrementDeltaTweetCount() {
		this.deltaTweetCount.incrementAndGet();
	}

	public void setTotalTweetCount(int ttc) {
		this.totalTweetCount.set(ttc);
	}

	public boolean isDone() {
		return done;
	}

	public TotalStatistics getParent() {
		return parent;
	}
}
