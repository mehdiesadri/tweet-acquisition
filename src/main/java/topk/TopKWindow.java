package topk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import conf.Tweet;

public class TopKWindow {
	final static Logger logger = LogManager.getLogger(TopKWindow.class
			.getName());
	long timestamp;
	int windowTweetCount;

	private Map<String, EntityBlock> entityBlocks;

	public TopKWindow(long ts) {
		timestamp = ts;
		windowTweetCount = 0;
		entityBlocks = new HashMap<String, EntityBlock>();
	}

	public void addEntity(Tweet tweet, String mention, String ents) {
		String[] es = ents.split("\t")[1].split(",");
		for (String e : es) {
			if (e.length() < 5)
				continue;

			e = e.trim();
			String[] e_parts = e.split("~~");
			String id = e_parts[0].trim();
			String title = e_parts[1].trim();
			Double rel = Double.valueOf(e_parts[2].trim());

			if (rel >= 1) {
				if (!entityBlocks.containsKey(title))
					entityBlocks.put(title, new EntityBlock(title));

				EntityBlock eb = entityBlocks.get(title);
				eb.matchingEntities.add(id);
				eb.mentions.put(tweet.getId() + "_" + mention, rel);
				tweet.addEntity(mention, ents);
			}
		}
	}

	public void printReport() {
		List<EntityBlock> ebs = new ArrayList<EntityBlock>(
				entityBlocks.values());

		for (EntityBlock entityBlock : ebs)
			if (entityBlock.mentions.size() <= 2)
				entityBlocks.remove(entityBlock.title);

		ebs = new ArrayList<EntityBlock>(entityBlocks.values());

		Collections.sort(ebs, new Comparator<EntityBlock>() {
			public int compare(EntityBlock o1, EntityBlock o2) {
				return o2.mentions.size() - o1.mentions.size();
			}
		});

		logger.info("##	" + timestamp + "	##	" + windowTweetCount);
		for (EntityBlock eblock : ebs.subList(0, 10)) {
			logger.info(eblock.title + "			" + eblock.mentions.size() + "			"
					+ eblock.matchingEntities.size());
		}
	}

	public int getEntityBlockSize() {
		return entityBlocks.size();
	}

	public void incrementWindowTweetCount() {
		windowTweetCount++;
	}
}
