package ta;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import stm.StorageManager;
import conf.ConfigMgr;
import conf.Interest;

public class MainTA {
	final static Logger logger = LogManager.getLogger(MainTA.class.getName());

	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String command = null;

		StorageManager.getInstance();
		// StorageManager.clearReports();
		// StorageManager.clearAll();

		List<Interest> interests = StorageManager.getInterests();
		Interest selectedInterest = null;

		String title = "";

		if (args.length > 0) {
			String interestId = args[0].trim();
			int pl = Integer.valueOf(args[1]);
			int ws = Integer.valueOf(args[2]);
			double rth = Double.valueOf(args[3]);
			double irrth = Double.valueOf(args[4]);

			title = "interest_" + interestId + "_pl_" + pl + "_ws_" + ws
					+ "_rth_" + rth + "_irrth_" + irrth;

			ConfigMgr.setConfigurationParameter("AcquisitionPhraseLimit",
					String.valueOf(pl));
			ConfigMgr.setConfigurationParameter("AcquisitionStartWindowSize",
					String.valueOf(ws));
			ConfigMgr.setConfigurationParameter(
					"AcquisitionTweetRelevanceThreshold", String.valueOf(rth));
			ConfigMgr.setConfigurationParameter(
					"AcquisitionTweetIrrelevanceThreshold",
					String.valueOf(irrth));

			for (Interest i : interests) {
				if (i.getId().equalsIgnoreCase(interestId)) {
					selectedInterest = i;
					break;
				}
			}
		} else {
			System.out
					.println("These are current active interests of the system: ");
			for (Interest interest : interests) {
				if (interest.isActive())
					System.out.println(interest.getId() + "\t"
							+ interest.getTopic());
			}

			System.out.println();

			while (selectedInterest == null) {
				System.out
						.println("Please enter the interest id you want to start the system with: ");
				command = br.readLine().trim();
				for (Interest i : interests) {
					if (i.getId().equalsIgnoreCase(command)) {
						selectedInterest = i;
						break;
					}
				}
			}

			title = "interest_"
					+ selectedInterest.getId()
					+ "_pl_"
					+ ConfigMgr
							.readConfigurationParameter("AcquisitionPhraseLimit")
					+ "_ws_"
					+ ConfigMgr
							.readConfigurationParameter("AcquisitionStartWindowSize")
					+ "_rth_"
					+ ConfigMgr
							.readConfigurationParameter("AcquisitionTweetRelevanceThreshold")
					+ "_irrth_"
					+ ConfigMgr
							.readConfigurationParameter("AcquisitionTweetIrrelevanceThreshold");
		}

		logger.info(title);

		Acquisition.getInstance();
		Acquisition.setInterest(selectedInterest);
		Acquisition.start();

		while (true) {
			try {
				command = br.readLine();
				if (command.equalsIgnoreCase("exit")
						|| command.equalsIgnoreCase("stop")) {
					Acquisition.stop();

					File file1 = new File("../logs/ta.log");
					File file2 = new File("../logs/" + title + ".log");
					if (file2.exists())
						throw new java.io.IOException("file exists");
					file1.renameTo(file2);

					StorageManager.close();
					break;
				}

				if (command.equalsIgnoreCase("smc"))
					System.out.println("Storage Manager Queue Size: "
							+ StorageManager.getQueueSize());

				if (command.equalsIgnoreCase("tc"))
					System.out.println("Total Tweet Count: "
							+ Acquisition.getInterest().getStatistics()
									.getTotalTweetCount());

				if (command.equalsIgnoreCase("wtc"))
					System.out
							.println("Window Tweet Count: "
									+ Acquisition.getCurrentWindow()
											.getStatistics().totalTweetCount
											.get());

				if (command.equalsIgnoreCase("tce"))
					System.out.println("Total English Tweet Count: "
							+ (Acquisition.getInterest().getStatistics()
									.getRelevantTweetCount() + Acquisition
									.getInterest().getStatistics()
									.getIrrelevantTweetCount()));

				if (command.equalsIgnoreCase("tcr"))
					System.out.println("Total Relevant Tweet Count: "
							+ (Acquisition.getInterest().getStatistics()
									.getRelevantTweetCount()));

				if (command.equalsIgnoreCase("wtcr"))
					System.out
							.println("Window Relevant Tweet Count: "
									+ (Acquisition.getCurrentWindow()
											.getStatistics().relevantTweetCount
											.get()));

				if (command.equalsIgnoreCase("tci"))
					System.out.println("Total Irrelevant Tweet Count: "
							+ (Acquisition.getInterest().getStatistics()
									.getIrrelevantTweetCount()));

				if (command.equalsIgnoreCase("wtci"))
					System.out
							.println("Window Irrelevant Tweet Count: "
									+ (Acquisition.getCurrentWindow()
											.getStatistics().irrelevantTweetCount
											.get()));

				if (command.equalsIgnoreCase("tcd"))
					System.out.println("Total Delta Tweet Count: "
							+ (Acquisition.getInterest().getStatistics()
									.getDeltaTweetCount()));

				if (command.equalsIgnoreCase("tcb"))
					System.out.println("Total Processing Buffer Tweet Count: "
							+ Acquisition.getCurrentWindow().getBufferSize());

				if (command.equalsIgnoreCase("ts"))
					System.out.println("Total Simulation Tweet Count: "
							+ Acquisition.getSimulator().getTotalCounter());

				if (command.equalsIgnoreCase("tsr"))
					System.out.println("Total Simulation Matched Tweet Count: "
							+ Acquisition.getSimulator().getMatchCounter());

				if (command.equalsIgnoreCase("gc"))
					System.gc();

			} catch (IOException ioe) {
				System.out.println("IO error trying to read your name!");
				System.exit(1);
			}
		}
	}
}