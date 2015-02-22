package ta;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;

import twitter4j.User;
import conf.Interest;
import conf.Tweet;

@Embedded("statistics")
public class UserStatistics {
	@Transient
	private long userId;

	private String screenName;
	private int relevantTweetCount;
	private int irrelevantTweetCount;

	private double avgRelevance;
	private double maxRelevance;
	private double minRelevance;

	public UserStatistics(long id) {
		userId = id;
		screenName = "";
		relevantTweetCount = 0;
		irrelevantTweetCount = 0;
		avgRelevance = 0;
		maxRelevance = 0;
		minRelevance = 0;
	}

	public void addTweet(Tweet tweet) {
		double r = tweet.getRelevance();
		if (r > maxRelevance)
			maxRelevance = r;
		if (r < minRelevance || minRelevance == 0)
			minRelevance = r;
		Interest interest = Acquisition.getInterest();
		if (r >= interest.getTweetRelevanceThreshold())
			relevantTweetCount++;
		else
			irrelevantTweetCount++;

		avgRelevance = ((double) (getTotalTweetCount() - 1) * avgRelevance + r)
				/ (double) getTotalTweetCount();

		User user = tweet.getStatus().getUser();
		if (!screenName.equals(user.getScreenName()))
			screenName = user.getScreenName();
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

	public long getUserId() {
		return userId;
	}
}
