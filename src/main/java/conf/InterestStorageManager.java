package conf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import stm.StorageManager;
import conf.Client;
import conf.Interest;
import conf.Phrase;

public class InterestStorageManager {

	public static void main(String[] args) {
		StorageManager.getInstance();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				String s = br.readLine();
				if (s.equals("help")) {
					System.out.println("help, show, rm iid, exit, user, add");
				}
				if (s.equals("show"))
					consolePrintInterests();
				if (s.startsWith("rm"))
					dropInterest(s.split(" ")[1].trim());
				if (s.equals("exit"))
					break;
				if (s.equals("add")) {
					consoleAddInterest(br);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void consoleAddInterest(BufferedReader br)
			throws IOException {
		System.out.println("- enter ClientId: ");
		String clientId = br.readLine().trim();
		System.out.println("- enter ClientName: ");
		String clientName = br.readLine().trim();
		System.out.println("- enter interestId: ");
		String interestId = br.readLine().trim();
		System.out.println("- enter interest topic name: ");
		String interestTopic = br.readLine().trim();

		Interest interest = new Interest(interestId, interestTopic);
		Client client = new Client(clientId, clientName);
		interest.setClient(client);
		interest.setActive(true);

		System.out
				.println("- enter phrases one by one or a file path containing phrases one per line: ");

		String phraseStr = "";
		while (true) {
			phraseStr = br.readLine();
			if (phraseStr == null || phraseStr.length() < 1)
				break;
			phraseStr = phraseStr.trim();
			File file = new File(phraseStr);
			if (file.exists()) {
				FileReader reader = new FileReader(file);
				br = new BufferedReader(reader);
				try {
					while (br.ready()) {
						String line = br.readLine();
						Phrase phrase = generatePhrase(line);
						interest.addPhrase(phrase);
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			} else
				interest.addPhrase(generatePhrase(phraseStr));
		}

		interest.setStartTime(System.currentTimeMillis() + 1000);
		StorageManager.storeInterest(interest);
	}

	private static Phrase generatePhrase(String phraseStr) {
		String[] phraseParts = phraseStr.split("[ \t]");
		double w = 1;

		Arrays.sort(phraseParts);
		phraseStr = "";

		for (String p : phraseParts) {
			p = p.trim();
			if (p.startsWith("-"))
				w = Double.valueOf(p.replaceAll("-", ""));
			else
				phraseStr += p + " ";
		}

		phraseStr = phraseStr.trim();

		Phrase phrase = new Phrase(phraseStr, w);
		phrase.setInitial(true);
		return phrase;
	}

	private static void consolePrintInterests() {
		List<Interest> interests = StorageManager.getInterests();
		System.out.println("active interests of the system are:");
		if (interests.size() == 0)
			System.out.println("there is no active interest in the system.");
		for (Interest interest : interests) {
			System.out.println("#interestId: " + interest.getId());
			System.out.println("	userID: " + interest.getClient().getId());
			System.out.println("	concept: " + interest.getTopic());
			System.out.println("	phrases: " + interest.getPhrases());
			System.out.println("	locations: " + interest.getLocations());
			System.out.println("	time: " + interest.getStartTime() + " to "
					+ interest.getEndTime());
			System.out.println("	active: " + interest.isActive());
			System.out.println();
		}
	}

	public static void dropInterest(String interestId) {
		StorageManager.deleteInterest(interestId);
	}
}
