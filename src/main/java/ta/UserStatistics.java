package ta;

import org.mongodb.morphia.annotations.Embedded;

import conf.Tweet;

@Embedded("statistics")
public class UserStatistics extends Statistics {
	private long userId;

	public UserStatistics(long id) {
		userId = id;
	}

	@Override
	public void addTweet(Tweet tweet) {
		super.addTweet(tweet);
	}

	public long getUserId() {
		return userId;
	}
}
