package ta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import stm.StorageManager;

public class MainTA {
	public static void main(String[] args) throws Exception {
		Acquisition.getInstance();
		Acquisition.start();

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String command = null;

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
							+ Acquisition.getTotalTweetCount());

				if (command.equalsIgnoreCase("tce"))
					System.out.println("Total Tweet Count so far is: "
							+ Acquisition.getCurrentWindow()
									.getTotalTweetCount());

				if (command.equalsIgnoreCase("tcr"))
					System.out.println("Total Tweet Count so far is: "
							+ Acquisition.getCurrentWindow().getStatistics()
									.getRelevantTweetCount());

				if (command.equalsIgnoreCase("tci"))
					System.out.println("Total Tweet Count so far is: "
							+ Acquisition.getCurrentWindow().getStatistics()
									.getIrrelevantTweetCount());

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