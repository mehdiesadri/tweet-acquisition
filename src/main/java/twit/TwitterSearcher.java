package twit;

//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//
//import twitter4j.GeoLocation;
//import twitter4j.QueryResult;
//import twitter4j.Tweet;
//import twitter4j.Twitter;
//import twitter4j.TwitterException;
//import twitter4j.TwitterFactory;
//import twitter4j.json.DataObjectFactory;
//
//
//import com.mongodb.DBCollection;
//import com.mongodb.DBObject;
//import com.mongodb.util.JSON;
//
//import config.Phrase;
//import config.Location;
//import config.Query;
//
public class TwitterSearcher {
    // private class TweetQueryPair {
    // public ArrayList<String> QueryIDs;
    // public Tweet Tweet;
    //
    // public TweetQueryPair() {
    // this.QueryIDs = new ArrayList<String>();
    // }
    // }
    //
    // private DBCollection DatabaseCollection;
    // private ArrayList<Query> Queries;
    //
    // // private boolean EnglishDetection;
    //
    // private ArrayList<TweetQueryPair> Results;
    //
    // public TwitterSearcher(ArrayList<Query> queries, DBCollection collection,
    // boolean englishDetection) {
    // Results = new ArrayList<TweetQueryPair>();
    // Queries = queries;
    // DatabaseCollection = collection;
    // // EnglishDetection = englishDetection;
    // }
    //
    // private void AddTweetQueryPair(Query query, Tweet tweet) {
    // for (TweetQueryPair tweetQueryPair : Results) {
    // if (tweetQueryPair.Tweet.getId() == tweet.getId()) {
    // tweetQueryPair.QueryIDs.add(query.ID);
    // return;
    // }
    // }
    //
    // TweetQueryPair twPair = new TweetQueryPair();
    // twPair.QueryIDs.add(query.ID);
    // twPair.Tweet = tweet;
    // Results.add(twPair);
    // }
    //
    // private String GetDate(Date date) {
    // java.util.GregorianCalendar calandar = new java.util.GregorianCalendar();
    // calandar.setTime(date);
    //
    // int year = calandar.get(Calendar.YEAR);
    // int month = calandar.get(Calendar.MONTH) + 1;
    // int day = calandar.get(Calendar.DAY_OF_MONTH);
    //
    // if (month > 9 && day > 9)
    // return year + "-" + month + "-" + day;
    // else if (month < 10 && day > 9)
    // return year + "-0" + month + "-" + day;
    // else if (month > 9 && day < 10)
    // return year + "-" + month + "-0" + day;
    // else
    // return year + "-0" + month + "-0" + day;
    // }
    //
    // private void OnMatchedTweet(ArrayList<String> MatchQs, Tweet tweet) {
    // String rawJason = DataObjectFactory.getRawJSON(tweet);
    // String prefixString = "{\"queries\": \"" + MatchQs
    // + "\", \"capturetime\":\"" + new Date() + "\"}";
    // String unitString = "{" + prefixString + " , " + rawJason + "}";
    // DBObject dbObj = (DBObject) JSON.parse(unitString);
    // DatabaseCollection.insert(dbObj);
    // }
    //
    // public void Search() {
    // Twitter twitter = new TwitterFactory().getInstance();
    //
    // for (Query query : Queries) {
    // String queryString = "";
    //
    // for (Phrase keyword : query.phrases) {
    // queryString = queryString + keyword.phrase + " ";
    // }
    //
    // twitter4j.Query q4j = new twitter4j.Query(queryString.trim());
    // q4j.setRpp(100);
    // q4j.setPage(15);
    //
    // q4j.setSince(GetDate(query.Since));
    // q4j.setUntil(GetDate(query.Until));
    //
    // QueryResult result = null;
    //
    // for (int i = 1; i < 16; i++) {
    // try {
    // result = twitter.search(q4j.page(i));
    // } catch (TwitterException e) {
    // e.printStackTrace();
    // }
    //
    // for (Tweet tweet : result.getTweets()) {
    // boolean matched = true;
    // GeoLocation tweetLocation = tweet.getGeoLocation();
    //
    // if (tweetLocation == null) {
    // AddTweetQueryPair(query, tweet);
    // continue;
    // }
    //
    // for (int j = 0; j < query.Locations.size(); j = j + 2) {
    // Location from = query.Locations.get(j);
    // Location to = query.Locations.get(j + 1);
    //
    // if (!(tweetLocation.getLatitude() >= from.Latitude
    // && tweetLocation.getLongitude() >= from.Longitude
    // && tweetLocation.getLatitude() <= to.Latitude && tweetLocation
    // .getLongitude() <= to.Longitude)) {
    // matched = false;
    // break;
    // }
    // }
    //
    // if (matched) {
    // AddTweetQueryPair(query, tweet);
    // }
    // }
    // }
    // }
    //
    // for (TweetQueryPair tweetQueryPair : Results) {
    // OnMatchedTweet(tweetQueryPair.QueryIDs, tweetQueryPair.Tweet);
    // }
    // }
}
