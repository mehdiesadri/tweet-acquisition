package ta;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import stm.StorageManager;
import topk.TopK;
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

	private List<String> topEntities;

	public Window() {
		done = false;
		tweetBuffer = new LinkedBlockingQueue<Tweet>();
		statistics = Acquisition.getInterest().getStatistics().addNewStat();
		executor = Executors.newFixedThreadPool(workerPoolSize);
		t_cw = new Thread(this);
		t_cw.setName("t_cw");
		topEntities = new LinkedList<String>();
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
			if (t_cw != null) {
				t_cw.interrupt();
				t_cw = null;
			}
			System.gc();
		}
		logger.info("window has been shutdown.");
	}

	public void addTweet(Tweet tweet) {
		int windowSize = Acquisition.getWindowSize();
		if (isOpen()
				&& (statistics.relevantTweetCount.get() >= windowSize)
				&& (Acquisition.isSimulating() || getLength() > Acquisition.minWindowLength)) {
			close();
		}

		// top-k
		TopK.enrich(tweet);

		if (isOpen())
			tweetBuffer.add(tweet);
		else {
			getStatistics().deltaTweetCount.incrementAndGet();
			tweet.setRelevance(.5);
			tweet.setInterestId(Acquisition.getInterest().getId());
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
		logger.info("WTC: " + statistics.totalTweetCount.get());
		int tetc = statistics.relevantTweetCount.get()
				+ statistics.irrelevantTweetCount.get();
		logger.info("WETC: " + tetc);
		logger.info("WRTC: " + statistics.relevantTweetCount);
		logger.info("WAvgRel: " + statistics.getAvgRelevance());
		logger.info("WMinRel: " + statistics.getMinRelevance());
		logger.info("WRHC: " + statistics.getRelevantHashtags().size());
		logger.info("WIHC: " + statistics.getIrrelevantHashtags().size());
		logger.info("TUC: " + Acquisition.getInterest().getUsers().size());
		logger.info("TTC: "
				+ Acquisition.getInterest().getStatistics()
						.getTotalTweetCount());
		logger.info("TRTC: "
				+ Acquisition.getInterest().getStatistics()
						.getRelevantTweetCount());
		logger.info("TDTC: "
				+ Acquisition.getInterest().getStatistics()
						.getDeltaTweetCount());
		if (Acquisition.isSimulating()) {
			logger.info("STC: " + Acquisition.getSimulator().getTotalCounter());
			logger.info("SRTC: " + Acquisition.getSimulator().getMatchCounter());
		}
	}

	public List<String> getTopEntities() {
		return topEntities;
	}

	public void setTopEntities(List<String> topEntities) {
		this.topEntities = topEntities;
	}
}