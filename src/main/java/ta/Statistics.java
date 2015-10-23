package ta;

import java.util.concurrent.atomic.AtomicInteger;

import org.mongodb.morphia.annotations.Transient;

import conf.Tweet;

public class Statistics {
	public volatile AtomicInteger totalTweetCount;
	public volatile AtomicInteger relevantTweetCount;
	public volatile AtomicInteger irrelevantTweetCount;
	public volatile AtomicInteger neutralTweetCount;
	public volatile AtomicInteger deltaTweetCount;

	public volatile double avgRelevance;
	public volatile double maxRelevance;
	public volatile double minRelevance;

	@Transient
	public double relevanceThreshold;
	@Transient
	public double irrelevanceThreshold;

	public Statistics() {
		totalTweetCount = new AtomicInteger(0);
		relevantTweetCount = new AtomicInteger(0);
		irrelevantTweetCount = new AtomicInteger(0);
		neutralTweetCount = new AtomicInteger(0);
		deltaTweetCount = new AtomicInteger(0);

		avgRelevance = 0;
		maxRelevance = 0;
		minRelevance = Double.MAX_VALUE;

		relevanceThreshold = Acquisition.getInterest()
				.getTweetRelevanceThreshold();
		irrelevanceThreshold = Acquisition.getInterest()
				.getTweetIrrelevanceThreshold();
	}

	public void addTweet(Tweet tweet) {
		synchronized (this) {
			totalTweetCount.incrementAndGet();
			updateRelevanceStatistics(tweet);
		}
	}

	private void updateRelevanceStatistics(Tweet tweet) {
		double r = tweet.getRelevance();

		if (r > relevanceThreshold)
			relevantTweetCount.incrementAndGet();
		else if (r < irrelevanceThreshold)
			irrelevantTweetCount.incrementAndGet();
		else
			neutralTweetCount.incrementAndGet();

		int tc = relevantTweetCount.get() + irrelevantTweetCount.get()
				+ neutralTweetCount.get();

		if (r > maxRelevance)
			maxRelevance = r;

		if (r <= minRelevance)
			minRelevance = r;

		avgRelevance = ((double) (tc - 1) * avgRelevance + r) / (double) tc;
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
}