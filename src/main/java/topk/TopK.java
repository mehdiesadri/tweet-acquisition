package topk;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import txt.TextNormalizer;
import conf.ConfigMgr;
import conf.Tweet;

public class TopK {
	private static final String category = "people";
	final static Logger logger = LogManager.getLogger(TopK.class.getName());

	private static TopKWindow window;
	private static String kb_host;
	private static int kb_port;
	private static Socket client;
	private static Map<String, String> kb_buffer;
	private static Map<String, Double> tc_buffer;

	public TopK() {
		kb_host = ConfigMgr.readConfigurationParameter("KBHost");
		kb_port = Integer.valueOf(ConfigMgr
				.readConfigurationParameter("KBPort"));
		kb_buffer = new HashMap<String, String>();
		tc_buffer = new HashMap<String, Double>();
		window = new TopKWindow(System.currentTimeMillis());
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

			if (getTermCommonness(tts) >= .6)
				continue;
			String ents = simpleLookup(tts);

			if (!tts.equals(ents.trim())) {
				window.addEntity(t, tts, ents);
				if (i + 1 < ts.size()) {
					t2 = t2 + "," + ts.get(i + 1);
					t3 = t2;
					ents = simpleLookup(t2);
					if (!t2.equals(ents.trim()))
						window.addEntity(t, t2, ents);
				}
				if (i + 2 < ts.size()) {
					t3 = t3 + "," + ts.get(i + 2);
					ents = simpleLookup(t3);
					if (!t3.equals(ents.trim()))
						window.addEntity(t, t3, ents);
				}
			}
		}

	}

	private static double getTermCommonness(String query) {
		if (tc_buffer.containsKey(query))
			return tc_buffer.get(query);
		String[] tc = TopK.executeCmd("tc~" + query).split("\t");
		Double commonness = tc.length > 0 ? Double.valueOf(tc[1].trim()) : 0;
		tc_buffer.put(query, commonness);
		return commonness;
	}

	private static String simpleLookup(String query) {
		if (kb_buffer.containsKey(query))
			return kb_buffer.get(query);
		String ents = TopK.executeCmd("gttl-" + category + "~" + query);
		kb_buffer.put(query, ents);
		return ents;
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
