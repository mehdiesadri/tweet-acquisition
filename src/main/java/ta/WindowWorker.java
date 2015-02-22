package ta;

import java.util.Collection;

import rc.RelevanceCheck;
import stm.StorageManager;
import conf.Interest;
import conf.Phrase;
import conf.Tweet;

public class WindowWorker implements Runnable {

	private int number;
	private Window window;
	private volatile Interest interest;

	public WindowWorker(Window w, int n) {
		number = n;
		window = w;
		interest = Acquisition.getInterest();
	}

	public void run() {
		Thread.currentThread().setName("t_ww" + number);

		while (window.isOpen() || window.getBufferSize() > 0) {
			Tweet tweet = window.pollTweet();

			if (tweet == null)
				continue;

			double rel = satisfyLanguage(tweet) ? RelevanceCheck.getRelevance(
					tweet, interest) : -1;
			tweet.setRelevance(rel);
			StorageManager.addTweet(tweet);
			updateStatistics(tweet);
		}
	}

	private boolean satisfyLanguage(Tweet tweet) {
		if (Acquisition.languageCheck)
			return Acquisition.languageClassifier.satisfy(tweet.getTerms());
		return true;
	}

	private void updateStatistics(Tweet tweet) {
		window.getStatistics().addTweet(tweet);

		if (tweet.getRelevance() < 0)
			return;

		Collection<Phrase> interestPhrases = Acquisition.getInterest()
				.getPhrases();

		long userId = tweet.getUserID();

		UserStatistics userStatistics = interest.getUserStatistics(userId);
		if (userStatistics == null) {
			interest.addUser(userId);
			userStatistics = interest.getUserStatistics(userId);
		}
		userStatistics.addTweet(tweet);

		for (Phrase phrase : interestPhrases) {
			if (tweet.containsPhrase(phrase.getText()))
				phrase.addTweet(tweet);
		}
	}
}
