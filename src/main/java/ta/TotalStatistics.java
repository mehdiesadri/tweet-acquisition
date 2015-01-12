package ta;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TotalStatistics {
	final static Logger logger = LogManager.getLogger(TotalStatistics.class
			.getName());

	private static final int maxNumStats = 3;

	private volatile int relevantTweetCount;
	private volatile int irrelevantTweetCount;

	private volatile LinkedList<WindowStatistics> stats;

	private volatile Map<String, Integer> relevantPatterns;
	private volatile Map<String, Integer> irrelevantPatterns;
	private volatile Map<String, Integer> relevantHashtags;
	private volatile Map<String, Integer> irrelevantHashtags;

	public TotalStatistics() {
		setRelevantTweetCount(0);
		setIrrelevantTweetCount(0);
		stats = new LinkedList<WindowStatistics>();
		relevantPatterns = new ConcurrentHashMap<String, Integer>();
		irrelevantPatterns = new ConcurrentHashMap<String, Integer>();
		relevantHashtags = new ConcurrentHashMap<String, Integer>();
		irrelevantHashtags = new ConcurrentHashMap<String, Integer>();
	}

	public double getTotalAvgRelevance() {
		double r = 0;
		int count = 0;
		for (WindowStatistics stat : stats) {
			count += stat.getTotalTweetCount();
			r += stat.getAvgRelevance() * (double) stat.getTotalTweetCount();
		}
		return r / (double) count;
	}

	public int getTotalTweetCount() {
		int count = 0;
		for (WindowStatistics stat : stats)
			count += stat.getTotalTweetCount();
		return count;
	}

	public int getTotalRelevantTweetCount() {
		int count = 0;
		for (WindowStatistics stat : stats)
			count += stat.getRelevantTweetCount();
		return count;
	}

	public int getTotalIrrelevantTweetCount() {
		int count = 0;
		for (WindowStatistics stat : stats)
			count += stat.getIrrelevantTweetCount();
		return count;
	}

	public synchronized void addStat() {
		WindowStatistics stat = new WindowStatistics();
		stats.push(stat);
		setRelevantTweetCount(getRelevantTweetCount()
				+ stat.getRelevantTweetCount());
		setIrrelevantTweetCount(getIrrelevantTweetCount()
				+ stat.getIrrelevantTweetCount());
		if (stats.size() > maxNumStats) {
			stats.removeLast();
			clean(relevantPatterns, relevantTweetCount);
			clean(irrelevantPatterns, irrelevantTweetCount);
			clean(relevantHashtags, relevantTweetCount);
			clean(irrelevantHashtags, irrelevantTweetCount);
		}

		System.gc();
	}

	private void clean(Map<String, Integer> hashmap, int totalCount) {
		List<String> toBeRemoved = new ArrayList<String>();
		for (String p : hashmap.keySet()) {
			if (((double) hashmap.get(p) / (double) totalCount) < .001)
				toBeRemoved.add(p);
		}

		for (String tbr : toBeRemoved)
			hashmap.remove(tbr);
	}

	public WindowStatistics getLastWindowStatistics() {
		if (stats.size() > 0)
			return stats.peek();
		return null;
	}

	public int getStatCount() {
		return stats.size();
	}

	public Map<String, Integer> getFrequentIrrelevantPatterns() {
		return irrelevantPatterns;
	}

	public Map<String, Integer> getFrequentRelevantPatterns() {
		return relevantPatterns;
	}

	public Map<String, Integer> getFrequentIrrelevantHashtags() {
		return irrelevantHashtags;
	}

	public Map<String, Integer> getFrequentRelevantHashtags() {
		return relevantHashtags;
	}

	public void addRelevantPattern(String key, Integer value) {
		relevantPatterns.put(key,
				relevantPatterns.containsKey(key) ? relevantPatterns.get(key)
						+ value : value);
	}

	public void addIrrelevantPattern(String key, Integer value) {
		irrelevantPatterns.put(
				key,
				irrelevantPatterns.containsKey(key) ? irrelevantPatterns
						.get(key) + value : value);
	}

	public void addRelevantHashtag(String key, Integer value) {
		relevantHashtags.put(key,
				relevantHashtags.containsKey(key) ? relevantHashtags.get(key)
						+ value : value);
	}

	public void addIrrelevantHashtag(String key, Integer value) {
		irrelevantHashtags.put(
				key,
				irrelevantHashtags.containsKey(key) ? irrelevantHashtags
						.get(key) + value : value);
	}

	public int getRelevantTweetCount() {
		return relevantTweetCount;
	}

	public void setRelevantTweetCount(int relevantTweetCount) {
		this.relevantTweetCount = relevantTweetCount;
	}

	public int getIrrelevantTweetCount() {
		return irrelevantTweetCount;
	}

	public void setIrrelevantTweetCount(int irrelevantTweetCount) {
		this.irrelevantTweetCount = irrelevantTweetCount;
	}
}
