package conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;

import ta.TotalStatistics;
import ta.WindowStatistics;

@Embedded
public class Phrase {
	private String text;
	private double weight;
	private boolean initial;
	private boolean active;

	@Transient
	private long lastUpdateTime;
	@Transient
	private TotalStatistics statistics;
	@Transient
	private HashSet<String> terms;

	// private Location location;

	public Phrase() {
		lastUpdateTime = 0;
		statistics = new TotalStatistics();
		active = true;
	}

	public Phrase(String txt, double wgt) {
		this();
		text = normalizePhraseText(txt);
		weight = wgt;
	}

	public boolean coverPhrase(String p) {
		// it means if p has all this phrase terms
		HashSet<String> pts = new HashSet<String>();
		for (String pt : p.split(" ")) {
			pt = pt.trim();
			pts.add(pt);
		}

		for (String tt : this.getTerms()) {
			if (!pts.contains(tt))
				return false;
		}

		return true;
	}

	public boolean containPhrase(String p) {
		String[] pterms = p.split(" ");

		for (String pt : pterms) {
			pt = pt.trim();
			if (!getTerms().contains(pt))
				return false;
		}

		return true;
	}

	public boolean equals(String p) {
		String[] pterms = p.split(" ");
		HashSet<String> tterms = this.getTerms();

		if (pterms.length != tterms.size())
			return false;

		for (String pt : pterms) {
			boolean hasTerm = false;
			for (String tt : tterms) {
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

	public HashSet<String> getTerms() {
		if (terms == null) {
			terms = new HashSet<String>();
			for (String term : getText().split(" ")) {
				term = term.trim();
				terms.add(term);
			}
		}

		return terms;
	}

	public String getText() {
		return text;
	}

	public double getWeight() {
		return weight;
	}

	public void setText(String txt) {
		this.text = normalizePhraseText(txt).toLowerCase();
	}

	public void setWeight(double wgt) {
		this.weight = wgt;
	}

	public double computeReward() {
		double reward = 0;
		reward += getStatistics().getTotalAvgRelevance()
				* (getStatistics().getIrrelevantTweetCount() + getStatistics()
						.getRelevantTweetCount());
		WindowStatistics lastWindowStatistics = getStatistics()
				.getLastWindowStatistics();
		if (lastWindowStatistics == null)
			return 0;
		reward += lastWindowStatistics.getAvgRelevance()
				* lastWindowStatistics.getTotalTweetCount();
		if (reward == 0)
			reward = 1;
		reward = reward * weight;
		return reward;
	}

	private String normalizePhraseText(String phraseText) {
		StringBuffer output = new StringBuffer();
		List<String> words = new ArrayList<String>();
		for (String word : phraseText.toLowerCase().split(" "))
			words.add(word.trim());

		Collections.sort(words);
		for (String word : words) {
			output.append(word);
			output.append(" ");
		}

		return output.toString().trim();
	}

	public boolean isInitial() {
		return initial;
	}

	public void setInitial(boolean initial) {
		this.initial = initial;
	}

	public long getLastUpdateTime() {
		return lastUpdateTime;
	}

	public void setLastUpdateTime(long updateTime) {
		this.lastUpdateTime = updateTime;
	}

	public TotalStatistics getStatistics() {
		return statistics;
	}

	public void addTweet(Tweet tweet) {
		WindowStatistics lastWindowStatistics = statistics
				.getLastWindowStatistics();
		if (lastWindowStatistics != null)
			lastWindowStatistics.addTweet(tweet);
	}

	public boolean hasTerm(String term) {
		return terms.contains(term);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}