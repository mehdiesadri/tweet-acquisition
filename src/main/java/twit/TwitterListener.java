package twit;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ta.Acquisition;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import util.SetUtil;
import conf.ConfigMgr;
import conf.Location;
import conf.Query;
import conf.Tweet;

public class TwitterListener implements Runnable {
	static final Logger logger = LogManager.getLogger(TwitterListener.class
			.getName());

	private TwitterStream twitterStream;
	private StatusListener listener;
	private FilterQuery filter;

	public TwitterListener() {
		Configuration conf = getConfiguration();
		twitterStream = new TwitterStreamFactory(conf).getInstance();
		filter = new FilterQuery();
	}

	private StatusListener initializeListener() {
		StatusListener listener = new StatusListener() {
			public void onDeletionNotice(StatusDeletionNotice arg0) {
			}

			public void onException(Exception arg0) {
			}

			public void onScrubGeo(long arg0, long arg1) {
			}

			public void onStatus(Status status) {
				Tweet tweet = new Tweet(status);
				Acquisition.OnStatus(tweet);
			}

			public void onTrackLimitationNotice(int arg0) {
			}

			public void onStallWarning(StallWarning arg0) {
			}
		};

		return listener;
	}

	private void StartListening() {
		listener = initializeListener();
		twitterStream.addListener(listener);

		Query query = Acquisition.getQuery();
		List<Location> locations = query.getLocations();

		ArrayList<String> filters = new ArrayList<String>();
		for (String phraseStr : query.getPhrases().keySet())
			filters.add(phraseStr);

		if (filters.size() > 0) {
			String[] filterArray = SetUtil.ListToArray(filters);
			filter.track(filterArray);
		} else
			return;

		double[][] locs = new double[locations.size() * 2][2];
		int index = 0;
		for (Location location : locations) {
			locs[index][0] = location.getSowthWestPoint().getX();
			locs[index][1] = location.getSowthWestPoint().getY();
			index++;
			locs[index][0] = location.getNorthEastPoint().getX();
			locs[index][1] = location.getNorthEastPoint().getY();
			index++;
		}

		filter.locations(locs);
		twitterStream.filter(filter);
	}

	public void stopListening() {
		twitterStream.shutdown();
	}

	public void run() {
		StartListening();
	}

	private Configuration getConfiguration() {
		ConfigurationBuilder confBuilder = new ConfigurationBuilder();
		confBuilder
				.setDebugEnabled(true)
				.setJSONStoreEnabled(true)
				.setOAuthConsumerKey(
						ConfigMgr.readConfigurationParameter("ConsumerKey"))
				.setOAuthConsumerSecret(
						ConfigMgr
								.readConfigurationParameter("ConsumerSecret"))
				.setOAuthAccessToken(
						ConfigMgr.readConfigurationParameter("AccessToken"))
				.setOAuthAccessTokenSecret(
						ConfigMgr
								.readConfigurationParameter("AccessTokenSecret"));

		Configuration twitter4jConf = confBuilder.build();
		return twitter4jConf;
	}
}
