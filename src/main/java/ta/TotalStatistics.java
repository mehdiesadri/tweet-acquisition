package ta;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.MapUtil;

public class TotalStatistics {
	final static Logger logger = LogManager.getLogger(TotalStatistics.class
			.getName());

	private volatile LinkedList<WindowStatistics> stats;

	private volatile AtomicInteger totalTweetCount;
	private volatile AtomicInteger relevantTweetCount;
	private volatile AtomicInteger irrelevantTweetCount;
	private volatile AtomicInteger deltaTweetCount;

	private volatile Map<String, Integer> relevantPatterns;
	private volatile Map<String, Integer> irrelevantPatterns;
	private volatile Map<String, Integer> relevantHashtags;
	private volatile Map<String, Integer> irrelevantHashtags;

	public TotalStatistics() {
		totalTweetCount = new AtomicInteger(0);
		relevantTweetCount = new AtomicInteger(0);
		irrelevantTweetCount = new AtomicInteger(0);
		deltaTweetCount = new AtomicInteger(0);

		stats = new LinkedList<WindowStatistics>();

		relevantPatterns = new ConcurrentHashMap<String, Integer>();
		irrelevantPatterns = new ConcurrentHashMap<String, Integer>();
		relevantHashtags = new ConcurrentHashMap<String, Integer>();
		irrelevantHashtags = new ConcurrentHashMap<String, Integer>();
	}

	public WindowStatistics getLastWindowStatistics() {
		if (stats.size() > 0)
			return stats.peek();
		return null;
	}

	public WindowStatistics addNewStat() {
		WindowStatistics newStat = new WindowStatistics(this);

		stats.push(newStat);

		if (stats.size() > Acquisition.maxNumberStats) {
			WindowStatistics rs = stats.removeLast();

			totalTweetCount.addAndGet(rs.totalTweetCount.get());
			deltaTweetCount.addAndGet(rs.deltaTweetCount.get());
			relevantTweetCount.addAndGet(rs.relevantTweetCount.get());
			irrelevantTweetCount.addAndGet(rs.irrelevantTweetCount.get());

			clean(relevantPatterns, getRelevantTweetCount());
			clean(irrelevantPatterns, getIrrelevantTweetCount());
			clean(relevantHashtags, getRelevantTweetCount());
			clean(irrelevantHashtags, getIrrelevantTweetCount());

			System.gc();
		}

		return newStat;
	}

	private void clean(Map<String, Integer> input, int totalCount) {
		synchronized (input) {
			List<Entry<String, Integer>> sortedEntryList = MapUtil
					.sortByValue(input);
			input.clear();

			for (int i = 0; i < Math.min(sortedEntryList.size(),
					Acquisition.maxNumberOfPatterns); i++) {
				String key = (String) sortedEntryList.get(i).getKey();
				int value = (int) sortedEntryList.get(i).getValue();
				double freq = (double) value / (double) totalCount;
				if (freq > Acquisition.newPhraseMinSup)
					input.put(key, value);
			}
		}
	}

	public int getStatCount() {
		return stats.size();
	}

	public double getTotalAvgRelevance() {
		int tweetcount = 0;
		double avgrel = 0;

		synchronized (this) {
			for (WindowStatistics stat : stats) {
				tweetcount += stat.totalTweetCount.get();
				avgrel += stat.getAvgRelevance()
						* (double) stat.totalTweetCount.get();
			}
		}
		return avgrel / (double) tweetcount;

	}

	public boolean isFull() {
		if (stats.size() >= Acquisition.maxNumberStats)
			return true;
		return false;
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
		int rtc = 0;
		synchronized (stats) {
			rtc = relevantTweetCount.get();
			for (WindowStatistics stat : stats)
				rtc += stat.relevantTweetCount.get();
		}
		return rtc;
	}

	public int getIrrelevantTweetCount() {
		int irtc = 0;
		synchronized (stats) {
			irtc = irrelevantTweetCount.get();
			for (WindowStatistics stat : stats)
				irtc += stat.irrelevantTweetCount.get();
		}
		return irtc;
	}

	public int getDeltaTweetCount() {
		int dtc = 0;
		synchronized (stats) {
			dtc = deltaTweetCount.get();
			for (WindowStatistics stat : stats)
				dtc += stat.deltaTweetCount.get();
		}
		return dtc;
	}

	public Integer getTotalTweetCount() {
		int ttc = 0;
		synchronized (stats) {
			ttc = totalTweetCount.get();
			for (WindowStatistics stat : stats)
				ttc += stat.totalTweetCount.get();
		}
		return ttc;
	}

	public Map<String, Integer> getRelevantPatterns() {
		return relevantPatterns;
	}

	public void setRelevantPatterns(
			Map<String, Integer> frequentRelevantPatterns) {
		this.relevantPatterns = frequentRelevantPatterns;
	}

	public Map<String, Integer> getIrrelevantPatterns() {
		return irrelevantPatterns;
	}

	public void setIrrelevantPatterns(
			Map<String, Integer> frequentIrrelevantPatterns) {
		this.irrelevantPatterns = frequentIrrelevantPatterns;
	}

	public Map<String, Integer> getRelevantHashtags() {
		return relevantHashtags;
	}

	public Map<String, Integer> getIrrelevantHashtags() {
		return irrelevantHashtags;
	}
}
