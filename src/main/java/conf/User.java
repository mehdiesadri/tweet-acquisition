package conf;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import ta.UserStatistics;

@Entity("user")
public class User {
	@Id
	private long id;
	private twitter4j.User userInfo;

	@Transient
	private UserStatistics statistics;

	public User(twitter4j.User u) {
		userInfo = u;
		if (u != null)
			id = getUserInfo().getId();
		statistics = new UserStatistics(id);
	}

	public twitter4j.User getUserInfo() {
		return userInfo;
	}

	public long getId() {
		return id;
	}

	public UserStatistics getStatistics() {
		return statistics;
	}
}
