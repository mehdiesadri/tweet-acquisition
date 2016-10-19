package twit;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.MongoClient;

import stm.StorageManager;
import ta.Acquisition;
import twitter4j.TwitterException;
import conf.ConfigMgr;
import conf.Interest;
import conf.JsonTweet;
import conf.Report;
import conf.Tweet;
import conf.User;

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

	public static void main(String[] args) throws TwitterException, IOException {
		Morphia morphia = new Morphia();

		Datastore datastore;
		Datastore simulationdatastore;

		morphia.map(Tweet.class).map(JsonTweet.class).map(User.class)
				.map(Interest.class).map(Report.class);

		MongoClient m = null;
		try {
			m = new MongoClient("sensoria.ics.uci.edu", 27017);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		simulationdatastore = morphia.createDatastore(m, "sample");
		simulationdatastore.ensureIndexes();

		datastore = morphia.createDatastore(m, "dataset");
		datastore.ensureIndexes();

		Iterator<Tweet> twts = simulationdatastore.createQuery(Tweet.class)
				.disableValidation().order("timestamp").iterator();

		if (twts == null)
			return;

		while (twts.hasNext()) {
			try {
				Tweet t = twts.next();
				JsonTweet tweet = new JsonTweet(t);
				datastore.save(tweet);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
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
