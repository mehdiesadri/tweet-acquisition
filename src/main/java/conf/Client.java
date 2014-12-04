package conf;

import java.util.ArrayList;
import java.util.List;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

@Embedded
public class Client {
	@Id
	private String id;
	private String name;

	@Transient
	private List<Interest> interests;
	@Transient
	private int numberOfHits;

	public Client() {
		interests = new ArrayList<Interest>();
	}

	public Client(String i, String n) {
		id = i;
		name = n;
		interests = new ArrayList<Interest>();
	}

	public int getNumberOfHits() {
		return numberOfHits;
	}

	public void setNumberOfHits(int numberOfHits) {
		this.numberOfHits = numberOfHits;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Interest> getInterests() {
		return interests;
	}

	public void setInterests(List<Interest> interests) {
		this.interests = interests;
	}
}