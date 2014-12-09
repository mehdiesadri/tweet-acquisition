package ta;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rc.RelevanceCheck;
import stm.StorageManager;
import conf.Interest;
import conf.Phrase;
import conf.Tweet;

public class Window implements Runnable {
	static final Logger logger = LogManager.getLogger(Window.class.getName());
	public Thread t_cw;

	private volatile BlockingQueue<Tweet> tweets;
	private volatile WindowStatistics statistics;
	private volatile long startTime;
	private volatile long endTime;
	private volatile boolean open;
	private volatile boolean done;

	private volatile Integer totalTweetCount;

	private volatile Interest interest;

	public Window() {
		totalTweetCount = 0;
		tweets = new LinkedBlockingQueue<Tweet>();
		interest = Acquisition.getInterest();
		interest.getStatistics().addStat();
		statistics = interest.getStatistics().getLastWindowStatistics();
		t_cw = new Thread(this);
		t_cw.setName("t_cw");
		done = false;
	}

	public void open() {
		startTime = System.currentTimeMillis();
		open = true;
		t_cw.start();
	}

	public void close() {
		synchronized (this) {
			open = false;
			endTime = System.currentTimeMillis();
			notifyAll();
		}
	}

	public boolean isOpen() {
		return open;
	}

	public void addTweet(Tweet tweet) {
		if (open)
			tweets.add(tweet);
		else {
			if (!Acquisition.languageCheck
					|| Acquisition.languageClassifier.satisfy(tweet.getTerms()))
				store(tweet);
		}

		synchronized (totalTweetCount) {
			totalTweetCount++;
		}
	}

	public long getEndTime() {
		return endTime;
	}

	public long getSize() {
		return (endTime > 0 ? endTime : System.currentTimeMillis()) - startTime;
	}

	public long getStartTime() {
		return startTime;
	}

	public WindowStatistics getStatistics() {
		return statistics;
	}

	public int getTotalTweetCount() {
		synchronized (totalTweetCount) {
			return totalTweetCount;
		}
	}

	public boolean isDone() {
		return done;
	}

	public void run() {
		int newUserCount = 0;

		Collection<Phrase> queryPhrases = Acquisition.getQuery().getPhrases()
				.values();

		while (open || (tweets.size() > 0)) {
			Tweet tweet = null;

			try {
				tweet = tweets.take();
				if (tweet == null)
					continue;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (Acquisition.languageCheck
					&& !Acquisition.languageClassifier
							.satisfy(tweet.getTerms())) {
				continue;
			}

			double r = RelevanceCheck.getRelevance(tweet, interest);
			tweet.setRelevance(r);
			if (r > interest.getTweetRelevanceThreshold())
				store(tweet);

			long userId = tweet.getUserID();
			UserStatistics userStatistics = interest.getUserStatistics(userId);
			if (userStatistics == null) {
				newUserCount++;
				interest.addUser(userId);
				userStatistics = interest.getUserStatistics(userId);
			}
			userStatistics.addTweet(tweet);

			for (Phrase phrase : queryPhrases) {
				if (tweet.containsPhrase(phrase.getText()))
					phrase.addTweet(tweet);
			}

			statistics.addTweet(tweet);

			synchronized (totalTweetCount) {
				if (totalTweetCount >= Acquisition.getWindowSize() && isOpen()
						&& (getSize() > 30 * 1000)) {
					close();
					logger.info("Window Size (seconds):	" + (double) getSize()
							/ 1000);
				}
			}
		}

		statistics.finalize(interest.getStatistics());
		for (Phrase phrase : queryPhrases)
			phrase.getStatistics().getLastWindowStatistics()
					.finalize(phrase.getStatistics());

		synchronized (this) {
			done = true;
			Acquisition.addTweetCount(totalTweetCount);
			notifyAll();
		}

		logger.info("Window Total Tweet Count: " + totalTweetCount);
		logger.info("Window English Tweet Count: "
				+ statistics.getTotalTweetCount());
		logger.info("Window Total English Relevant Tweet Count: "
				+ statistics.getRelevantTweetCount());
		logger.info("Window Total English Irrelevant Tweet Count: "
				+ statistics.getIrrelevantTweetCount());
		logger.info("Window Avg  Tweet Relevance: "
				+ statistics.getAvgRelevance());
		logger.info("Window Min  Tweet Relevance: "
				+ statistics.getMinRelevance());
		logger.info("Window Max  Tweet Relevance: "
				+ statistics.getMaxRelevance());
		logger.info("Window Relevant English Tweet Count: "
				+ statistics.getRelevantTweetCount());
		logger.info("Window New User Count: " + newUserCount);
		logger.info("Window Relevant Hashtag Count: "
				+ statistics.getRelevantHashtags().size());
		logger.info("Window Irrelevant Hashtag Count: "
				+ statistics.getIrrelevantHashtags().size());
		logger.info("Total User Count: " + interest.getUsers().size());
	}

	private void store(Tweet tweet) {
		StorageManager.addTweet(tweet);
		if (tweet.getStatus().getRetweetedStatus() != null) {
			Tweet retweet = new Tweet(tweet.getStatus().getRetweetedStatus());
			StorageManager.addTweet(retweet);
		}
	}
}