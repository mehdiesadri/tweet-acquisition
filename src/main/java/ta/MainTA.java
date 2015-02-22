package ta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import conf.Interest;
import stm.StorageManager;

public class MainTA {
	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String command = null;

		StorageManager.getInstance();
		// StorageManager.clearReports();
		StorageManager.clearAll();

		Acquisition.getInstance();

		List<Interest> interests = StorageManager.getInterests();
		Interest selectedInterest = null;

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

		Acquisition.setInterest(selectedInterest);
		Acquisition.start();

		while (true) {
			try {
				command = br.readLine();
				if (command.equalsIgnoreCase("exit")
						|| command.equalsIgnoreCase("stop")) {
					Acquisition.stop();
					break;
				}

				if (command.equalsIgnoreCase("smc"))
					System.out
							.println("Number of tweets in storage manager queue is: "
									+ StorageManager.getQueueSize());

				if (command.equalsIgnoreCase("tc"))
					System.out.println("Total Tweet Count so far is: "
							+ Acquisition.getInterest().getStatistics()
									.getTotalTweetCount());

				if (command.equalsIgnoreCase("tce"))
					System.out.println("Total Tweet Count so far is: "
							+ (Acquisition.getInterest().getStatistics()
									.getRelevantTweetCount() + Acquisition
									.getInterest().getStatistics()
									.getIrrelevantTweetCount()));

				if (command.equalsIgnoreCase("tcr"))
					System.out.println("Total Tweet Count so far is: "
							+ (Acquisition.getInterest().getStatistics()
									.getRelevantTweetCount()));

				if (command.equalsIgnoreCase("tci"))
					System.out.println("Total Tweet Count so far is: "
							+ (Acquisition.getInterest().getStatistics()
									.getIrrelevantTweetCount()));

				if (command.equalsIgnoreCase("tcd"))
					System.out.println("Total Tweet Count so far is: "
							+ (Acquisition.getInterest().getStatistics()
									.getDeltaTweetCount()));

				if (command.equalsIgnoreCase("tcb"))
					System.out.println("Total Tweet Count so far is: "
							+ Acquisition.getCurrentWindow().getBufferSize());

				if (command.equalsIgnoreCase("gc"))
					System.gc();

			} catch (IOException ioe) {
				System.out.println("IO error trying to read your name!");
				System.exit(1);
			}
		}
	}
}