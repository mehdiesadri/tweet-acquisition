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

public class Window {
	static final Logger logger = LogManager.getLogger(Window.class.getName());

	private Thread t_cw;
	private volatile BlockingQueue<Tweet> tweets;
	private volatile WindowStatistics statistics;

	private volatile long startTime;
	private volatile long endTime;
	private volatile boolean open;
	private volatile boolean done;

	private volatile Interest interest;
	private volatile Integer totalTweetCount;

	public Window() {
		totalTweetCount = 0;
		done = false;
		interest = Acquisition.getInterest();
		tweets = new LinkedBlockingQueue<Tweet>();
		statistics = new WindowStatistics();
		t_cw = new Thread(this);
		t_cw.setName("t_cw");
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
		synchronized (totalTweetCount) {
			totalTweetCount++;
		}

		if (open)
			tweets.add(tweet);
	}

	public long getEndTime() {
		return endTime;
	}

	public long getLength() {
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

	public String getInterestId() {
		return interest.getId();
	}

	public void run() {
		while (open || getBufferSize() > 0) {
			Tweet tweet = null;

			try {
				tweet = tweets.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (Acquisition.languageCheck
					&& !Acquisition.languageClassifier
							.satisfy(tweet.getTerms())) {
				continue;
			}

			double rel = RelevanceCheck.getRelevance(tweet, interest);
			tweet.setRelevance(rel);
			// store(tweet);

			updateStatistics(tweet);

			synchronized (totalTweetCount) {
				if (totalTweetCount >= Acquisition.getWindowSize()
						&& isOpen()
						&& (Acquisition.isSimulating() || getLength() > Acquisition.MinWindowLength)) {
					close();
					logger.info("Window Size (seconds):	"
							+ (double) getLength() / 1000);
				}
			}
		}

		finalizeStatistics();
		synchronized (this) {
			done = true;
			Acquisition.addTweetCount(totalTweetCount);
			notifyAll();
		}

		printReport();
		System.gc();
	}

	private void finalizeStatistics() {
		Collection<Phrase> interestPhrases = Acquisition.getInterest()
				.getPhrases();

		statistics.finalize(interest.getStatistics(), getLength());

		for (Phrase phrase : interestPhrases) {
			TotalStatistics pstatistics = phrase.getStatistics();
			if (pstatistics != null) {
				WindowStatistics plastWindowStatistics = pstatistics
						.getLastWindowStatistics();
				if (plastWindowStatistics != null)
					plastWindowStatistics.finalize(pstatistics, getLength());
			}
		}
	}

	private void updateStatistics(Tweet tweet) {
		Collection<Phrase> interestPhrases = Acquisition.getInterest()
				.getPhrases();
		long userId = tweet.getUserID();

		UserStatistics userStatistics = interest.getUserStatistics(userId);
		if (userStatistics == null) {
			interest.addUser(userId);
			userStatistics = interest.getUserStatistics(userId);
		}
		userStatistics.addTweet(tweet);

		for (Phrase phrase : interestPhrases) {
			if (tweet.containsPhrase(phrase.getText()))
				phrase.addTweet(tweet);
		}

		statistics.addTweet(tweet);
	}

	private void store(Tweet tweet) {
		StorageManager.addTweet(tweet);
		if (tweet.getStatus().getRetweetedStatus() != null) {
			Tweet retweet = new Tweet(tweet.getStatus().getRetweetedStatus());
			StorageManager.addTweet(retweet);
		}
	}

	private void printReport() {
		logger.info("Window Tweet Count: " + totalTweetCount);
		logger.info("Window English Tweet Count: "
				+ statistics.getTotalTweetCount());
		logger.info("Window Total English Relevant Tweet Count: "
				+ statistics.getRelevantTweetCount());
		logger.info("Window Avg  Tweet Relevance: "
				+ statistics.getAvgRelevance());
		logger.info("Window Min  Tweet Relevance: "
				+ statistics.getMinRelevance());
		logger.info("Window Relevant Hashtag Count: "
				+ statistics.getRelevantHashtags().size());
		logger.info("Window Irrelevant Hashtag Count: "
				+ statistics.getIrrelevantHashtags().size());
		logger.info("Total User Count: " + interest.getUsers().size());
	}

	public int getBufferSize() {
		return tweets.size();
	}
}