package stm;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import conf.ConfigMgr;
import conf.Interest;
import conf.Location;
import conf.Phrase;
import conf.Query;
import conf.Report;
import conf.Tweet;
import conf.User;

public class StorageManager implements Runnable {
	private static StorageManager instance = null;

	private volatile static BlockingQueue<Tweet> tweets;
	private volatile Morphia morphia = new Morphia();
	private volatile static boolean storeUserInfo = false;
	private volatile static Datastore datastore;

	private static boolean running;
	private static Thread t_sm;

	public StorageManager() {
		this(ConfigMgr.readConfigurationParameter("MongoDBDatabase"));
	}

	public StorageManager(String database) {
		storeUserInfo = Boolean.valueOf(ConfigMgr
				.readConfigurationParameter("MongoDBStoreUserInfo"));
		String host = ConfigMgr
				.readConfigurationParameter("MongoDBDatabaseHost");
		int port = Integer.parseInt(ConfigMgr
				.readConfigurationParameter("MongoDBDatabasePort"));

		Mongo m = null;
		try {
			m = new MongoClient(host, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		morphia.map(Query.class).map(Tweet.class).map(User.class)
				.map(Interest.class).map(Report.class);
		datastore = morphia.createDatastore(m, database);
		tweets = new LinkedBlockingQueue<Tweet>();
	}

	public synchronized static StorageManager getInstance() {
		if (instance == null) {
			instance = new StorageManager();
			StorageManager.t_sm = new Thread(instance);
			StorageManager.t_sm.setName("t_sm");
		}
		return instance;
	}

	public static void start() {
		running = true;
		t_sm.start();
	}

	public static void stop() {
		running = false;
	}

	public void run() {
		while (running) {
			Tweet tweet = null;
			try {
				tweet = tweets.take();
				if (tweet == null)
					continue;
				storeTweet(tweet);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized static void addTweet(Tweet tweet) {
		tweets.add(tweet);
	}

	public static void storeInterest(Interest interest) {
		datastore.save(interest);
	}

	public static void deleteInterest(String interestId) {
		datastore.delete(Interest.class, interestId);
	}

	public static void storeQuery(Query query) {
		datastore.save(query);
	}

	public static void storeUser(User user) {
		datastore.save(user);
	}

	public static void storeReport(Report report) {
		datastore.save(report);
	}

	public static void resetUserWindowCounts() {
		if (storeUserInfo) {
			datastore.update(
					datastore.createQuery(User.class),
					datastore.createUpdateOperations(User.class).set(
							"windowNumTweets", 0));
		}
	}

	public static List<Interest> getInterests() {
		List<Interest> interests = datastore.find(Interest.class)
				.field("active").equal(true).asList();
		return interests;
	}

	public static List<Query> getQueries() {
		List<Query> queries = datastore.find(Query.class).asList();
		return queries;
	}

	public void main(String[] args) throws Exception {
		Query q = new Query();
		q.setStartTime(765);
		Phrase phrase = new Phrase();
		phrase.setText("iran");
		q.addPhrase(phrase);
		q.addLocation(new Location("meh", "tehran"));
		storeQuery(q);

		Tweet t = new Tweet(null);
		storeTweet(t);
	}

	public static void removeAll() {
		datastore.delete(datastore.createQuery(User.class));
		datastore.delete(datastore.createQuery(Query.class));
		datastore.delete(datastore.createQuery(Report.class));
		datastore.delete(datastore.createQuery(Tweet.class));
	}

	public static void close() {
		running = false;
		for (Tweet tweet : tweets)
			storeTweet(tweet);
	}

	private static void storeTweet(Tweet tweet) {
		datastore.save(tweet);

		if (storeUserInfo) {
			// storeUser(user);
		}
	}

	public synchronized static int getQueueSize() {
		return tweets.size();
	}

}
