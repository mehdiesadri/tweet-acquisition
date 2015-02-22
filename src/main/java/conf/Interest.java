package conf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

import ta.TotalStatistics;
import ta.UserStatistics;

@Entity("interest")
public class Interest {
	@Id
	private volatile String id;
	@Embedded
	private volatile Client client;
	private volatile String topic;

	@Embedded
	private volatile Map<String, Phrase> phrases;

	@Embedded
	private volatile List<Location> locations;

	private volatile boolean active;

	private volatile Long endTime;
	private volatile long startTime;

	@Transient
	private volatile Map<String, Double> termFreq;
	@Transient
	private volatile Map<Long, UserStatistics> users;
	@Transient
	private volatile double weightSum;
	@Transient
	private volatile TotalStatistics statistics;
	@Transient
	private volatile double tweetRelevanceThreshold;
	@Transient
	private volatile double tweetIrrelevanceThreshold;
	@Transient
	private double minPhraseTermFreq = 0;

	public Interest(String i, String t) {
		this();
		id = i;
		topic = t;
	}

	public Interest() {
		tweetRelevanceThreshold = Double
				.valueOf(ConfigMgr
						.readConfigurationParameter("AcquisitionTweetRelevanceThreshold"));
		tweetIrrelevanceThreshold = Double
				.valueOf(ConfigMgr
						.readConfigurationParameter("AcquisitionTweetIrrelevanceThreshold"));

		phrases = new ConcurrentHashMap<String, Phrase>();
		users = new ConcurrentHashMap<Long, UserStatistics>();
		locations = new ArrayList<Location>();
		statistics = new TotalStatistics();

		active = true;
	}

	public void addPhrase(Phrase phrase) {
		if (!phrases.containsKey(phrase.getText()))
			phrases.put(phrase.getText(), phrase);
	}

	public void removePhrase(Phrase phrase) {
		if (!phrase.isInitial())
			phrases.remove(phrase.getText());
	}

	public synchronized void computeFrequencies() {
		weightSum = 0;
		for (Phrase phrase : getPhrases())
			weightSum += phrase.getWeight();

		termFreq = new HashMap<String, Double>();
		for (Phrase phrase : getPhrases()) {
			for (Phrase p : getPhrases()) {
				if (p != phrase && p.coverPhrase(phrase.getText())) {
					phrase.setActive(false);
					break;
				}
			}

			for (String term : phrase.getTerms()) {
				double d = phrase.getWeight() / weightSum;
				if (!termFreq.containsKey(term)) {
					termFreq.put(term, d);
					if (d < minPhraseTermFreq || minPhraseTermFreq == 0)
						minPhraseTermFreq = d;
				} else {
					double freq = termFreq.get(term) + d;
					termFreq.put(term, freq);
					if (freq < minPhraseTermFreq || minPhraseTermFreq == 0)
						minPhraseTermFreq = freq;
				}
			}
		}
	}

	public double getPhraseTermFreq(String term) {
		if (termFreq == null)
			computeFrequencies();
		if (termFreq.containsKey(term))
			return termFreq.get(term);
		return 0;
	}

	public double getMinPhraseTermFreq() {
		return minPhraseTermFreq;
	}

	public boolean hasPhrase(String p) {
		String phrase = "";
		String[] pparts = p.split(" ");
		Arrays.sort(pparts);
		for (String t : pparts)
			phrase += t + " ";
		phrase = phrase.trim();
		return phrases.containsKey(phrase);
	}

	public boolean coverPhrase(String phrase) {
		for (Phrase p : phrases.values()) {
			if (p.coverPhrase(phrase))
				return true;
		}

		return false;
	}

	public boolean phraseCovers(Phrase phrase) {
		for (Phrase p : phrases.values()) {
			if (phrase.coverPhrase(p.getText()))
				return true;
		}

		return false;
	}

	public String getId() {
		return String.valueOf(id);
	}

	public double getWeightSum() {
		return weightSum;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public String getTopic() {
		return topic;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public Collection<Phrase> getPhrases() {
		return phrases.values();
	}

	public List<Location> getLocations() {
		return locations;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Map<String, Double> getPhraseTermFreq() {
		return termFreq;
	}

	public TotalStatistics getStatistics() {
		return statistics;
	}

	public long getOldestPhraseUpdateTime() {
		long oldestPhraseUpdateTime = startTime;
		for (Phrase phrase : getPhrases()) {
			if (phrase.isActive()
					&& phrase.getLastUpdateTime() < oldestPhraseUpdateTime
					|| oldestPhraseUpdateTime == startTime)
				oldestPhraseUpdateTime = phrase.getLastUpdateTime();
		}

		return oldestPhraseUpdateTime;
	}

	public double getTweetRelevanceThreshold() {
		return tweetRelevanceThreshold;
	}

	public double getTweetIrrelevanceThreshold() {
		return tweetIrrelevanceThreshold;
	}

	public Map<Long, UserStatistics> getUsers() {
		return users;
	}

	public void addUser(long id) {
		users.put(id, new UserStatistics(id));
	}

	public UserStatistics getUserStatistics(long userId) {
		if (users.containsKey(userId))
			return users.get(userId);
		return null;
	}
}