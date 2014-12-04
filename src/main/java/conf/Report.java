package conf;

import java.util.HashMap;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("report")
public class Report {
	@Id
	private ObjectId id;
	private String interestId;
	private int tweetCount;
	private long startTime;
	private long endTime;
	private HashMap<Integer, HashMap<String, Double>> itemsets;
	private HashMap<String, Integer> terms;

	public Report() {
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public int getTweetCount() {
		return tweetCount;
	}

	public void setTweetCount(int tweetCount) {
		this.tweetCount = tweetCount;
	}

	public String getInterestId() {
		return interestId;
	}

	public void setInterestId(String interestId) {
		this.interestId = interestId;
	}

	public HashMap<Integer, HashMap<String, Double>> getItemsets() {
		return itemsets;
	}

	public void setItemsets(HashMap<Integer, HashMap<String, Double>> itemsets) {
		this.itemsets = itemsets;
	}

	public HashMap<String, Integer> getSingles() {
		return terms;
	}

	public void setSingles(HashMap<String, Integer> singles) {
		this.terms = singles;
	}
}
