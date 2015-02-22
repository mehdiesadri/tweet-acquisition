package ta;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import stm.StorageManager;
import conf.Interest;
import conf.Phrase;
import conf.Tweet;

public class Window implements Runnable {
	static final Logger logger = LogManager.getLogger(Window.class.getName());

	private static final int executerTimeOutHours = 5;
	private static final int workerPoolSize = 5;
	private static final int pollTimeout = 10;

	private Thread t_cw;

	private volatile ExecutorService executor;
	private volatile WindowStatistics statistics;
	private volatile BlockingQueue<Tweet> tweetBuffer;

	private volatile long startTime;
	private volatile long endTime;
	private volatile boolean open;
	private volatile boolean done;

	public Window() {
		done = false;
		tweetBuffer = new LinkedBlockingQueue<Tweet>();
		statistics = Acquisition.getInterest().getStatistics().addNewStat();
		executor = Executors.newFixedThreadPool(workerPoolSize);
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
		logger.info("Window Size (secs):	" + (double) getLength() / 1000);
	}

	public void finish() {
		synchronized (this) {
			done = true;
			finalizeStatistics();
			notifyAll();
		}
		logger.info("window has been finished.");
	}

	public void shutdown() {
		synchronized (this) {
			printReport();
			t_cw.interrupt();
			t_cw = null;
			System.gc();
		}
		logger.info("window has been shutdown.");
	}

	public void addTweet(Tweet tweet) {
		if (isOpen()
				&& (statistics.getRelevantTweetCount() >= Acquisition
						.getWindowSize() || !Acquisition.getInterest()
						.getStatistics().isFull())
				&& (Acquisition.isSimulating() || getLength() > Acquisition.minWindowLength)) {
			close();
		}

		if (isOpen())
			tweetBuffer.add(tweet);
		else {
			getStatistics().incrementDeltaTweetCount();
			tweet.setRelevance(.5);
			StorageManager.addTweet(tweet);
		}
	}

	public void run() {
		for (int i = 0; i < workerPoolSize; i++) {
			WindowWorker worker = new WindowWorker(this, i);
			executor.execute(worker);
		}

		executor.shutdown();
		try {
			if (!executor
					.awaitTermination(executerTimeOutHours, TimeUnit.HOURS))
				logger.info("Threads didn't finish in " + executerTimeOutHours
						+ " hours!");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		finish();
	}

	public Tweet pollTweet() {
		try {
			return tweetBuffer.poll(pollTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean isOpen() {
		return open;
	}

	public int getBufferSize() {
		return tweetBuffer.size();
	}

	private void finalizeStatistics() {
		Collection<Phrase> interestPhrases = Acquisition.getInterest()
				.getPhrases();

		Interest interest = Acquisition.getInterest();
		getStatistics().finalize(interest.getStatistics(), getLength());

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

	public boolean isDone() {
		return done;
	}

	private void printReport() {
		logger.info("Window Tweet Count: " + statistics.getTotalTweetCount());
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
		logger.info("Total User Count: "
				+ Acquisition.getInterest().getUsers().size());
		logger.info("Total tweet count: "
				+ Acquisition.getInterest().getStatistics()
						.getTotalTweetCount());
		logger.info("Total relevant tweet count: "
				+ Acquisition.getInterest().getStatistics()
						.getRelevantTweetCount());
		logger.info("Total delta tweet count: "
				+ Acquisition.getInterest().getStatistics()
						.getDeltaTweetCount());
	}
}