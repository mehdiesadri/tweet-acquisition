package conf;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import ta.WindowStatistics;

@Entity("report")
public class Report {
	@Id
	private ObjectId id;
	private String interestId;
	private long startTime;
	private long endTime;
	private Integer totalTweetCount;
	private WindowStatistics statistics;

	public Report(ta.Window window) {
		startTime = window.getStartTime();
		endTime = window.getEndTime();
		interestId = window.getInterestId();
		totalTweetCount = window.getTotalTweetCount();
		statistics = window.getStatistics();
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

	public String getInterestId() {
		return interestId;
	}

	public void setInterestId(String interestId) {
		this.interestId = interestId;
	}
}
