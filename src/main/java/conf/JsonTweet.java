package conf;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import twitter4j.Status;

@Entity(value = "tweet", noClassnameStored = true)
public class JsonTweet {
	@Id
	private long id;

	private Status status;
	private double relevance;

	@Indexed
	private long timestamp;

	public JsonTweet() {
	}

	public JsonTweet(Tweet t) {
		relevance = t.getRelevance();
		status = t.getStatus();
		if (status != null) {
			id = status.getId();
			timestamp = status.getCreatedAt().getTime();
		}
	}
}
