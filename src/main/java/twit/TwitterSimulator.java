package twit;
//package sim;
//
//import java.net.UnknownHostException;
//
//import stm.DB;
//import ta.Consumer;
//
//import com.mongodb.DBCollection;
//import com.mongodb.DBCursor;
//import com.mongodb.DBObject;
//import com.mongodb.Mongo;
//import com.mongodb.MongoClient;
//import com.mongodb.util.JSON;
//
//import conf.ConfigMgr;
//import conf.Tweet;
//
//public class TwitterSimulator {
//
//	DBCollection Collection;
//	Consumer DataConsumer;
//
//	public TwitterSimulator(DBCollection collectionSource, Consumer consumer) {
//		Collection = collectionSource;
//		DataConsumer = consumer;
//	}
//
//	public static void simulate() {
//		Mongo mongoSource;
//		try {
//			String host = ConfigMgr
//					.readConfigurationParameter("SimulationDBHost");
//			mongoSource = new MongoClient(host);
//			String dbname = ConfigMgr
//					.readConfigurationParameter("SimulationDBName");
//			String colname = ConfigMgr
//					.readConfigurationParameter("SimulationDBCollectionName");
//			DB srcDB = mongoSource.getDB(dbname);
//			DBCollection srcCol = srcDB.getCollection(colname);
//			TwitterSimulator twitterSimulator = new TwitterSimulator(srcCol,
//					getConsumer());
//			String limit = ConfigMgr
//					.readConfigurationParameter("SimulationLimit");
//			twitterSimulator.start(Integer.valueOf(limit));
//		} catch (UnknownHostException e) {
//			logger.trace(e);
//		}
//	}
//
//	public void start(int limit) {
//		DBObject obj = (DBObject) JSON.parse("{\"header.tweet_time\":1}");
//		DBCursor cursor = Collection.find().sort(obj);
//
//		long previousTweetTime = 0;
//
//		int num = 0;
//		while (num < limit) {
//			cursor.next();
//			DBObject tweetObject = cursor.curr();
//			DBObject object = (DBObject) tweetObject.get("header");
//			long tweetTime = Long.valueOf((String) object.get("tweet_time"));
//
//			try {
//				long delay = tweetTime - previousTweetTime;
//				if (delay > 0 && delay < 100)
//					Thread.sleep(delay);
//				previousTweetTime = tweetTime;
//
//				DBObject tweetObj = (DBObject) tweetObject.get("tweet");
//				Tweet tweet = new Tweet(Long.valueOf(tweetObj.get("id")
//						.toString()));
//				DataConsumer.OnStatus(tweet);
//				num++;
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}
//}
