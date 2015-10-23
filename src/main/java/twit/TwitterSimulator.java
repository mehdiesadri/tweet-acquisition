package twit;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import stm.StorageManager;
import ta.Acquisition;
import conf.ConfigMgr;
import conf.Tweet;

public class TwitterSimulator implements Runnable {
	final static Logger logger = LogManager.getLogger(TwitterSimulator.class
			.getName());

	private Boolean running;
	private static Thread t_sim;
	private AtomicInteger totalCounter;
	private AtomicInteger matchCounter;
	private Iterator<Tweet> tweets;
	private conf.Query query;

	public TwitterSimulator() {
		StorageManager.getInstance();
		tweets = StorageManager.getSimulationQuery().iterator();
		totalCounter = new AtomicInteger(0);
		matchCounter = new AtomicInteger(0);
		t_sim = new Thread(this);
		t_sim.setName("t_sim");
		running = false;
	}

	public void start() {
		if (!isRunning()) {
			query = Acquisition.getQuery();

			running = true;
			if (!t_sim.isAlive()) {
				t_sim = new Thread(this);
				t_sim.setName("t_sim");
				t_sim.start();
			}

			synchronized (this) {
				notifyAll();
			}
		}
	}

	public void stop() {
		if (isRunning())
			running = false;
	}

	public void run() {
		if (tweets == null)
			return;

		int limit = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("SimulationLimit"));

		while (tweets.hasNext() && (limit == -1 || matchCounter.get() < limit)) {
			if (!isRunning()) {
				try {
					synchronized (this) {
						wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			Tweet tweet = tweets.next();
			totalCounter.incrementAndGet();

			if (query.satisfy(tweet)) {
				matchCounter.incrementAndGet();
				Acquisition.OnStatus(tweet);
			}
		}

		Acquisition.getCurrentWindow().close();
		Acquisition.getCurrentWindow().shutdown();
		Acquisition.stop();

		running = false;
	}

	public int getTotalCounter() {
		return totalCounter.get();
	}

	public int getMatchCounter() {
		return matchCounter.get();
	}

	public Boolean isRunning() {
		return running;
	}
}
