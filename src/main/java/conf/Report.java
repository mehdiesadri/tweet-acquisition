package conf;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import ta.Acquisition;
import ta.WindowStatistics;

@Entity("report")
public class Report {
	@Id
	private ObjectId id;
	private String interestId;
	private long startTime;
	private long endTime;
	private long duration;

	@Embedded
	private Query query;

	@Embedded
	private WindowStatistics statistics;

	public Report(ta.Window window, Query q) {
		startTime = window.getStartTime();
		endTime = window.getEndTime();
		statistics = window.getStatistics();
		interestId = Acquisition.getInterest().getId();
		setDuration(window.getLength());
		query = q;
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

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}
}
