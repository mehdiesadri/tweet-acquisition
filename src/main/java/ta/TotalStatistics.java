package ta;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TotalStatistics {
	private static final int maxNumStats = 10;
	LinkedList<WindowStatistics> stats;
	private Map<String, Integer> relevantPatterns;
	private Map<String, Integer> irrelevantPatterns;

	public TotalStatistics() {
		stats = new LinkedList<WindowStatistics>();
		relevantPatterns = new ConcurrentHashMap<String, Integer>();
		irrelevantPatterns = new ConcurrentHashMap<String, Integer>();
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
		if (stats.size() > maxNumStats)
			stats.removeLast();
	}

	public WindowStatistics getLastWindowStatistics() {
		if (stats.size() > 0)
			return stats.peek();
		return null;
	}

	public int getStatCount() {
		return stats.size();
	}

	public void getRelevantPatterns() {
		for (WindowStatistics stat : stats) {
			Map<String, Integer> x = stat.getFrequentRelevantPatterns();
		}
	}

	public Map<String, Integer> getFrequentIrrelevantPatterns() {
		return irrelevantPatterns;
	}

	public Map<String, Integer> getFrequentRelevantPatterns() {
		return relevantPatterns;
	}

	public void addRelevant(String key, Integer value) {
		relevantPatterns.put(key,
				relevantPatterns.containsKey(key) ? relevantPatterns.get(key)
						+ value : value);
	}

	public void addIrrelevant(String key, Integer value) {
		irrelevantPatterns.put(
				key,
				irrelevantPatterns.containsKey(key) ? irrelevantPatterns
						.get(key) + value : value);
	}
}
