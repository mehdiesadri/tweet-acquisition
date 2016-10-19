package topk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import txt.TextNormalizer;
import conf.ConfigMgr;
import conf.Tweet;

public class TopK {
	private static final String category = "politicians";
	final static Logger logger = LogManager.getLogger(TopK.class.getName());

	private static LinkedList<TopKWindow> windows;
	private static int WindowSize = 500;
	private static int SlideSize = 100;
	private static String kb_host;
	private static int kb_port;
	private static Socket client;

	public TopK() {
		kb_host = ConfigMgr.readConfigurationParameter("KBHost");
		kb_port = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("KBPort"));
		windows = new LinkedList<TopKWindow>();
		for (int i = 0; i < (double) WindowSize / (double) SlideSize; i++)
			windows.add(new TopKWindow(System.currentTimeMillis() + i));
	}

	// top-k thing
	public static void enrich(Tweet t) {
		String tt = t.getStatus().getText();
		String[] ttParts = tt.split(" ");
		tt = "";
		for (String tp : ttParts) {
			if (!tp.startsWith("@") && !tp.startsWith("http")
					&& !tp.startsWith("shttp")
					&& !tp.toLowerCase().equals("via"))
				tt = tt + " " + tp;
		}

		List<String> ts = TextNormalizer.normalize(tt);
		for (int i = 0; i < ts.size(); i++) {
			String tts = ts.get(i);
			String t2 = tts;
			String t3 = tts;

			String[] tc = TopK.executeCmd("tc~" + tts).split("\t");
			Double commonness = tc.length > 0 ? Double.valueOf(tc[1].trim())
					: 0;
			if (commonness >= .6)
				continue;

			String ents = TopK.executeCmd("gttl-" + category + "~" + tts);
			if (!tts.equals(ents.trim())) {
				addEntity(t, tts, ents);
				if (i + 1 < ts.size()) {
					t2 = t2 + "," + ts.get(i + 1);
					t3 = t2;
					ents = TopK.executeCmd("gttl-" + category + "~" + t2);
					if (!t2.equals(ents.trim())) {
						addEntity(t, t2, ents);
					}
				}
				if (i + 2 < ts.size()) {
					t3 = t3 + "," + ts.get(i + 2);
					ents = TopK.executeCmd("gttl-" + category + "~" + t3);
					if (!t3.equals(ents.trim())) {
						addEntity(t, t3, ents);
					}
				}
			}
		}

		for (int i = 0; i < (double) WindowSize / (double) SlideSize; i++) {
			if (i > windows.size() - 1)
				windows.add(new TopKWindow(System.currentTimeMillis()));
			windows.get(i).incrementWindowTweetCount();
		}
	}

	private static void addEntity(Tweet tweet, String mention, String ents) {
		if (windows.getFirst().windowTweetCount <= WindowSize) {
			for (int i = 0; i < (double) WindowSize / (double) SlideSize; i++) {
				if (windows.getFirst().windowTweetCount > i
						* ((double) SlideSize))
					windows.get(i).addEntity(tweet, mention, ents);
			}
		} else {
			windows.removeFirst().printReport();
			windows.addLast(new TopKWindow(System.currentTimeMillis()));
		}
	}

	public static String executeCmd(String term) {
		try {
			client = new Socket(kb_host, kb_port);
			DataOutputStream out = new DataOutputStream(
					client.getOutputStream());
			out.writeUTF(term.trim());
			InputStream inFromServer = client.getInputStream();
			DataInputStream in = new DataInputStream(inFromServer);
			String response = in.readUTF();
			client.close();
			return response;
		} catch (IOException e) {
			logger.info(e.getMessage());
		}

		return null;
	}
}
