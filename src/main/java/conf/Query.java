package conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;

@Embedded("query")
public class Query {
	@Id
	private ObjectId id;

	@Embedded
	private List<Location> locations;
	private List<Long> users;

	@Embedded
	private HashMap<String, Phrase> phrases;

	public Query() {
		phrases = new HashMap<String, Phrase>();
		locations = new ArrayList<Location>();
		users = new ArrayList<Long>();
	}

	public void addLocation(Location location) {
		this.locations.add(location);
	}

	public void addPhrase(Phrase phrase) {
		this.phrases.put(phrase.getText(), phrase);
	}

	public void addUser(long userId) {
		this.users.add(userId);
	}

	public List<Location> getLocations() {
		return locations;
	}

	public HashMap<String, Phrase> getPhrases() {
		return phrases;
	}

	public void removePhrase(String qPhraseText) {
		phrases.remove(qPhraseText);
	}

	public void setLocations(ArrayList<Location> locations) {
		this.locations = locations;
	}

	public boolean satisfy(Tweet tweet) {
		for (String pText : phrases.keySet()) {
			if (tweet.containsPhrase(pText)) {
				return true;
			}
		}
		return false;
	}
}