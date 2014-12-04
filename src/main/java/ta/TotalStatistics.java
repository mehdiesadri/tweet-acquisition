package ta;

import java.util.LinkedList;

public class TotalStatistics {
	private static final int maxNumStats = 10;
	LinkedList<WindowStatistics> stats;

	public TotalStatistics() {
		stats = new LinkedList<WindowStatistics>();
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
}
