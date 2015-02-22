package ta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

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

	public static final int maxNumberOfPatterns = 1000000;
	public static double percentageOfNewPhrasesToAdd;
	public static int minNumberOfTweets;
	public static double newPhraseMinSup;
	public static Integer newPhraseMaxLength;
	public static long minWindowLength;
	public static double minNewPhraseScore;
	public static Integer phraseLimit;
	public static double eefraction;
	public static int locationLimit;
	public static int userLimit;
	public static int maxNumberStats;

	public static boolean running;
	public volatile static Boolean languageCheck;
	public volatile static LanguageClassifier languageClassifier;

	private static TwitterSimulator simulator;
	private static final String termCommonnessHost = "128.195.53.246";
	private static final int termCommonnessPort = 9090;

	private volatile static Acquisition instance = null;
	private volatile static long lastInterestUpdateTime;
	private volatile static TwitterListener listener;
	private volatile static Interest interest;
	private volatile static int windowSize;
	private volatile static Window window;
	private volatile static Query query;
	private static boolean simulating;

	private static Thread t_ac;
	private static Thread t_tl;

	public Acquisition() {
		t_ac = new Thread(this);
		t_ac.setName("t_ac");

		languageCheckInitialization();

		minWindowLength = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionMinWindowLength")) * 60 * 1000;
		windowSize = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionStartWindowSize"));
		simulating = Boolean.valueOf(ConfigMgr
				.readConfigurationParameter("UseSimulator"));
		minNumberOfTweets = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionMinNumberOfTweets"));
		minNewPhraseScore = Double.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionMinNewPhraseScore"));
		newPhraseMinSup = Double.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionNewPhraseMinSup"));
		newPhraseMaxLength = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionNewPhraseMaxLength"));
		percentageOfNewPhrasesToAdd = Double
				.valueOf(ConfigMgr
						.readConfigurationParameter("AcquisitionPercentageOfNewPhrasesToAdd"));
		eefraction = Double.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionEEFraction"));

		phraseLimit = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionPhraseLimit"));
		locationLimit = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionLocationLimit"));
		userLimit = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionUserLimit"));

		maxNumberStats = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("AcquisitionMaxNumberStats"));

		if (isSimulating())
			simulator = new TwitterSimulator();

		running = false;

		TextNormalizer.getInstance();
		QueryGenerator.getInstance();
		InterestUpdater.getInstance();

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
				query = QueryGenerator.generate(getInterest());

				window = new Window();
				window.open();

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

					if (getInterest().getOldestPhraseUpdateTime() >= lastInterestUpdateTime
							|| interest.getStatistics().isFull())
						InterestUpdater.update();
					else
						InterestUpdater.quickUpdate();

					window.shutdown();
				}

				Report report = new Report(window, query);
				StorageManager.storeReport(report);

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

	public static void setInterest(Interest interest) {
		Acquisition.interest = interest;
		Acquisition.interest.computeFrequencies();
	}
}
