package conf;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import ta.UserStatistics;

@Entity("user")
public class User {
	@Id
	private long id;

	@Embedded
	private twitter4j.User userInfo;

	@Embedded
	private UserStatistics statistics;

	public User(twitter4j.User u, UserStatistics s) {
		userInfo = u;
		statistics = s;
		if (u != null)
			id = getUserInfo().getId();
	}

	public long getId() {
		return id;
	}

	public twitter4j.User getUserInfo() {
		return userInfo;
	}

	public UserStatistics getStatistics() {
		return statistics;
	}
}
