package conf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Entity {
	Double confidence;
	HashSet<String> mentions;

	Map<String, String> gttl_titles;
	Map<String, Double> gttl_scores;

	Map<String, String> gtl_titles;
	Map<String, Double> gtl_scores;

	public Entity() {
		confidence = 0.0;
		mentions = new HashSet<String>();
		gttl_titles = new HashMap<String, String>();
		gttl_scores = new HashMap<String, Double>();
		gtl_titles = new HashMap<String, String>();
		gtl_scores = new HashMap<String, Double>();
	}

	public void addMention(String mention) {
		if (!mentions.contains(mention))
			mentions.add(mention);
	}

	public Boolean hasMention(String mention) {
		return mentions.contains(mention);
	}

	public void importGttlResults(String result) {
		String[] mainParts = result.split("	");
		if (mainParts.length < 1)
			return;

		addMention(mainParts[0].trim());
		String[] ents = mainParts[1].split(",");
		for (int i = 0; i < ents.length; i++) {
			String ent = ents[i];
			String[] entParts = ent.split("~~");
			while (entParts.length != 3) {
				ent = ent + "," + ents[i + 1];
				i++;
				entParts = ent.split("~~");
			}

			gttl_titles.put(entParts[0].trim(), entParts[1].trim());
			gttl_scores.put(entParts[0].trim(),
					Double.valueOf(entParts[2].trim()));
		}

	}
}
