package ta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

import lang.EnglishClassifier;
import lang.LanguageClassifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import qg.InterestUpdater;
import qg.QueryGenerator;
import stm.StorageManager;
import twit.TwitterListener;
import twit.TwitterSimulator;
import txt.TextNormalizer;
import conf.ConfigMgr;
import conf.Interest;
import conf.Query;
import conf.Report;
import conf.Tweet;

public class Acquisition implements Runnable {
	final static Logger logger = LogManager.getLogger(Acquisition.class
			.getName());

	private static final String termCommonnessHost = "128.195.53.246";
	private static final int termCommonnessPort = 9090;
	public static final long MinWindowLength = 5 * 60 * 1000;

	private volatile static Acquisition instance = null;
	public static boolean running;
	public volatile static Boolean languageCheck;
	public volatile static LanguageClassifier languageClassifier;

	private volatile static long lastInterestUpdateTime;
	private volatile static TwitterListener listener;
	private volatile static Interest interest;
	private volatile static int windowSize;
	private volatile static Window window;
	private volatile static Query query;
	private static boolean simulating;
	private static TwitterSimulator simulator;
	private volatile static int tweetCount;

	private static Thread t_ac;
	private static Thread t_tl;

	public Acquisition() {
		t_ac = new Thread(this);
		t_ac.setName("t_ac");

		languageCheckInitialization();
		windowSize = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionStartWindowSize"));
		simulating = Boolean.valueOf(ConfigMgr
				.readConfigurationParameter("UseSimulator"));

		if (isSimulating())
			simulator = new TwitterSimulator();

		running = false;
		tweetCount = 0;

		TextNormalizer.getInstance();
		QueryGenerator.getInstance();
		StorageManager.getInstance();
		StorageManager.removeAll();

		List<Interest> interests = StorageManager.getInterests();
		if (interests != null && interests.size() > 0)
			interest = interests.get(0);

		interest.computeFrequencies();

		logger.info("tweet acquisition has been initiated.");
	}

	public synchronized static Acquisition getInstance() {
		if (instance == null) {
			instance = new Acquisition();
		}
		return instance;
	}

	public void run() {
		StorageManager.start();

		lastInterestUpdateTime = System.currentTimeMillis();
		running = true;

		while (running) {
			try {
				query = QueryGenerator.generate(interest);
				StorageManager.storeQuery(query);
				startNewWindow();

				if (isSimulating()) {
					simulator.start();
					logger.info("Simulation is started.");
				} else {
					startNewListener();
					logger.info("Listening is started.");
				}

				synchronized (window) {
					while (window.isOpen())
						window.wait();
					logger.info("current window has been closed.");

					while (!window.isDone())
						window.wait();
					logger.info("window processing has been done.");
				}

				Report report = new Report(getCurrentWindow());
				StorageManager.storeReport(report);

				if (interest.getOldestPhraseUpdateTime() >= lastInterestUpdateTime)
					InterestUpdater.update(interest);

				stopListener();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

	private void stopListener() {
		if (listener != null) {
			listener.stopListening();
			t_tl.interrupt();
		}
	}

	public static void start() {
		t_ac.start();
	}

	public static void stop() {
		if (running) {
			running = false;

			listener.stopListening();
			t_tl.interrupt();

			window.close();
			StorageManager.stop();
		}
	}

	public synchronized static void startNewListener() {
		listener = new TwitterListener();
		t_tl = new Thread(listener);
		t_tl.setName("t_tl");
		t_tl.start();
	}

	public synchronized static void startNewWindow() {
		window = new Window();
		window.open();
	}

	public synchronized static void OnStatus(Tweet tweet) {
		window.addTweet(tweet);
	}

	public synchronized static Window getCurrentWindow() {
		return window;
	}

	public static Interest getInterest() {
		return interest;
	}

	public static Query getQuery() {
		return query;
	}

	private void languageCheckInitialization() {
		languageCheck = Boolean.parseBoolean(ConfigMgr
				.readConfigurationParameter("LanguageCheck"));
		if (languageCheck) {
			LanguageClassifier languageClassifier = null;
			String language = ConfigMgr
					.readConfigurationParameter("LanguageCheckLanguage");
			if (language.equals("en")) {
				String threshold = ConfigMgr
						.readConfigurationParameter("LanguageCheckThreshold");
				languageClassifier = EnglishClassifier.getInstance(Double
						.valueOf(threshold));
			}
			Acquisition.languageClassifier = languageClassifier;
		}
	}

	public static int getWindowSize() {
		return windowSize;
	}

	public static void setWindowSize(int windowSize) {
		Acquisition.windowSize = windowSize;
	}

	public static int getTotalTweetCount() {
		return tweetCount + (window.isDone() ? 0 : window.getTotalTweetCount());
	}

	public static void addTweetCount(int c) {
		tweetCount += c;
	}

	public static double getTermCommonness(String term) {
		try {
			Socket client = new Socket(termCommonnessHost, termCommonnessPort);
			DataOutputStream out = new DataOutputStream(
					client.getOutputStream());
			out.writeUTF(term.trim());
			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);
			String response = in.readUTF();
			String[] responseParts = response.split("\t");
			client.close();
			return Double.valueOf(responseParts[1]);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

		return .5;
	}

	public static boolean isSimulating() {
		return simulating;
	}
}
