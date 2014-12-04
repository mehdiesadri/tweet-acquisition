package conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity("query")
public class Query {
	@Id
	private ObjectId id;

	private long startTime;
	private long endTime;

	@Embedded
	private List<Location> locations;
	@Embedded
	private HashMap<String, Phrase> phrases;

	public Query() {
		phrases = new HashMap<String, Phrase>();
		locations = new ArrayList<Location>();
	}

	public void addLocation(Location location) {
		this.locations.add(location);
	}

	public void addPhrase(Phrase phrase) {
		this.phrases.put(phrase.getText(), phrase);
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
}