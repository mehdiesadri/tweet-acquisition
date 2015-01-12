package twit;

import org.mongodb.morphia.query.Query;

import stm.StorageManager;
import ta.Acquisition;
import conf.Tweet;

public class TwitterSimulator implements Runnable {

	private boolean started;
	private static Thread t_sim;

	public TwitterSimulator() {
		StorageManager.getInstance();
		t_sim = new Thread(this);
		t_sim.setName("t_sim");
		started = false;
	}

	public void start() {
		if (!started)
			t_sim.start();
		started = true;
	}

	public void run() {
		Query<Tweet> query = StorageManager.getSimulationQuery();
		if (query == null)
			return;
		for (Tweet tweet : query) {
			conf.Query acquisitionQuery = Acquisition.getQuery();
			if (acquisitionQuery.satisfy(tweet))
				Acquisition.OnStatus(tweet);
		}
	}
}
