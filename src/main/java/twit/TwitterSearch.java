package twit;

public class TwitterSearch {
    // public void start() {
    // Configuration conf = getConfiguration();
    //
    // QueryExtractor queryExtractor = new QueryExtractor();
    // ArrayList<Category> categories = queryExtractor
    // .ExtractQueries(filePath);
    // search(config, queryAnalyser, collection);
    // }
    //
    // private static void search(Config config, QueryAnalyser queryAnalyser,
    // DBCollection collection) {
    // Runnable runnable = new searchThread(queryAnalyser.SearchQueries,
    // collection, config.ActiveEnglishDetection);
    // Thread thread = new Thread(runnable);
    // thread.start();
    // }
    //
    // class searchThread extends Thread {
    // private boolean ActiveEnglishDetection;
    // private DBCollection DatabaseCollection;
    // public ArrayList<Query> SearchQueries;
    //
    // public searchThread(ArrayList<Query> queries, DBCollection collection,
    // boolean activeEnglishDetection) {
    // SearchQueries = queries;
    // DatabaseCollection = collection;
    // ActiveEnglishDetection = activeEnglishDetection;
    // }
    //
    // @Override
    // public void run() {
    // TwitterSearcher twitterSearcher = new TwitterSearcher(
    // SearchQueries, DatabaseCollection, ActiveEnglishDetection);
    // twitterSearcher.Search();
    // }
    // }
}
