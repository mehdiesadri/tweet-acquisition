package twit;

import java.io.IOException;
import java.util.HashSet;

import stm.StorageManager;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import conf.ConfigMgr;
import conf.Tweet;

public class TwitterSampler {

	public static void main(String[] args) throws TwitterException, IOException {
		StorageManager.getInstance(ConfigMgr
				.readConfigurationParameter("MongoDBSampleDatabase"));
		StorageManager.start();
		String threshold = ConfigMgr
				.readConfigurationParameter("LanguageCheckThreshold");
		final lang.LanguageClassifier languageClassifier = new lang.EnglishClassifier(
				Double.valueOf(threshold));

		Configuration conf = getConfiguration();

		StatusListener listener = new StatusListener() {
			public void onStatus(Status status) {
				HashSet<String> words = new HashSet<String>();
				for (String w : status.getText().split(" "))
					words.add(w);

				if (languageClassifier.satisfy(words)) {
					Tweet tweet = new Tweet(status);
					StorageManager.storeTweet(tweet);
				}
			}

			public void onDeletionNotice(
					StatusDeletionNotice statusDeletionNotice) {
			}

			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
			}

			public void onException(Exception ex) {
				ex.printStackTrace();
			}

			public void onScrubGeo(long arg0, long arg1) {
			}

			public void onStallWarning(StallWarning arg0) {
			}
		};

		TwitterStream twitterStream = new TwitterStreamFactory(conf)
				.getInstance();
		twitterStream.addListener(listener);
		// sample() method internally creates a thread which manipulates
		// TwitterStream and calls these adequate listener methods continuously.
		twitterStream.sample();
	}

	private static Configuration getConfiguration() {
		ConfigurationBuilder confBuilder = new ConfigurationBuilder();
		confBuilder
				.setDebugEnabled(true)
				.setJSONStoreEnabled(true)
				.setOAuthConsumerKey(
						ConfigMgr.readConfigurationParameter("ConsumerKey"))
				.setOAuthConsumerSecret(
						ConfigMgr.readConfigurationParameter("ConsumerSecret"))
				.setOAuthAccessToken(
						ConfigMgr.readConfigurationParameter("AccessToken"))
				.setOAuthAccessTokenSecret(
						ConfigMgr
								.readConfigurationParameter("AccessTokenSecret"));

		Configuration twitter4jConf = confBuilder.build();
		return twitter4jConf;
	}
}
