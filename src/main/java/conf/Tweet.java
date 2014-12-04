package conf;

import java.util.Date;
import java.util.List;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import twitter4j.Status;
import txt.TextNormalizer;

@Entity("tweet")
public class Tweet {
	@Id
	private long id;
	private Status status;

	@Transient
	private double relevance;
	private List<String> terms;

	public Tweet(Status s) {
		relevance = 0;
		status = s;
		if (getStatus() != null)
			id = getStatus().getId();
	}

	public boolean containsPhrase(String p) {
		String[] pterms = p.split(" ");

		for (String pt : pterms) {
			boolean hasTerm = false;
			for (String tt : this.getTerms()) {
				if (pt.equals(tt)) {
					hasTerm = true;
					break;
				}
			}

			if (!hasTerm)
				return false;
		}

		return true;
	}

	public List<String> getTerms() {
		if (terms == null) {
			String text = getStatus().getText();
			terms = TextNormalizer.normalize(text);
		}
		return terms;
	}

	public String getText() {
		return getStatus().getText();
	}

	public Date getTime() {
		return getStatus().getCreatedAt();
	}

	public long getTimeStamp() {
		return getTime().getTime();
	}

	public long getUserID() {
		if (status != null)
			return getStatus().getUser().getId();
		return -1;
	}

	public long getId() {
		return id;
	}

	public Status getStatus() {
		return status;
	}

	public double getRelevance() {
		return relevance;
	}

	public void setRelevance(double relevance) {
		this.relevance = relevance;
	}
}
