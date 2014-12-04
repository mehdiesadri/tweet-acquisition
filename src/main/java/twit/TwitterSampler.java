//package twit;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import stm.StorageManager;
//import ta.Acquisition;
//import twitter4j.StallWarning;
//import twitter4j.Status;
//import twitter4j.StatusDeletionNotice;
//import twitter4j.StatusListener;
//import twitter4j.TwitterException;
//import twitter4j.TwitterStream;
//import twitter4j.TwitterStreamFactory;
//import twitter4j.conf.Configuration;
//import conf.ConfigManager;
//import conf.Tweet;
//
//public class TwitterSampler {
//	private static StorageManager storageManager;
//
//	public static void main(String[] args) throws TwitterException, IOException {
//		storageManager = new StorageManager(
//				ConfigManager
//						.readConfigurationParameter("MongoDBSampleDatabase"));
//		String threshold = ConfigManager
//				.readConfigurationParameter("LanguageCheckThreshold");
//		String dicPath = ConfigManager
//				.readConfigurationParameter("LanguageCheckDictionaryPath");
//		final nlp.LanguageClassifier languageClassifier = new nlp.EnglishClassifier(
//				Double.valueOf(threshold), dicPath);
//
//		Configuration conf = Acquisition.getConfiguration();
//
//		StatusListener listener = new StatusListener() {
//			public void onStatus(Status status) {
//				List<String> words = new ArrayList<String>();
//				for (String w : status.getText().split(" "))
//					words.add(w);
//
//				if (languageClassifier.satisfy(words)) {
//					Tweet tweet = new Tweet(status);
//					storageManager.storeTweet(tweet);
//				}
//			}
//
//			public void onDeletionNotice(
//					StatusDeletionNotice statusDeletionNotice) {
//			}
//
//			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
//			}
//
//			public void onException(Exception ex) {
//				ex.printStackTrace();
//			}
//
//			public void onScrubGeo(long arg0, long arg1) {
//			}
//
//			public void onStallWarning(StallWarning arg0) {
//			}
//		};
//
//		TwitterStream twitterStream = new TwitterStreamFactory(conf)
//				.getInstance();
//		twitterStream.addListener(listener);
//		// sample() method internally creates a thread which manipulates
//		// TwitterStream and calls these adequate listener methods continuously.
//		twitterStream.sample();
//	}
//
// }
