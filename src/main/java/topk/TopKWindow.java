package topk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import conf.Tweet;

public class TopKWindow implements Runnable {
	final static Logger logger = LogManager.getLogger(TopKWindow.class
			.getName());

	private Integer windowSize = 2000;
	private Integer slideSize = 500;

	private Queue<Tweet> windowTweetOrder;
	private List<Tweet> slideBuffer;;
	private Map<String, EntityBlock> entityBlocks;

	long timestamp;
	int tweetCount;

	Thread t;

	public TopKWindow(long ts) {
		timestamp = ts;
		tweetCount = 0;
		entityBlocks = new HashMap<String, EntityBlock>();
		slideBuffer = new ArrayList<Tweet>();
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
				if (tweetCount < windowSize) {
					if (!entityBlocks.containsKey(title))
						entityBlocks.put(title, new EntityBlock(title));

					EntityBlock eb = entityBlocks.get(title);
					// eb.matchingEntities.add(id);
					eb.addMention(tweet.getId(), rel, true);
					tweet.addEntity(mention, ents);
				} else {
					tweet.addEntity(mention, ents);
					slideBuffer.add(tweet);
				}

				if (tweetCount >= windowSize) {
					// window finished, report
					logger.info("window is full");
//					logger.info(entityBlocks.size());
//					logger.info(tweetCount);
//					logger.info(slideBuffer.size());

					run();

					if (t == null) {
						t = new Thread(this, String.valueOf(System
								.currentTimeMillis()));
						t.start();
					}

					tweetCount = 0;
				}
			}
		}

		tweetCount++;
	}

	public void printReport() {
		List<EntityBlock> ebs = new ArrayList<EntityBlock>(
				entityBlocks.values());

		for (EntityBlock entityBlock : ebs)
			// if (entityBlock.mentions.size() <= 2)
			entityBlocks.remove(entityBlock.getTitle());

		ebs = new ArrayList<EntityBlock>(entityBlocks.values());

		// Collections.sort(ebs, new Comparator<EntityBlock>/() {
		// public int compare(EntityBlock o1, EntityBlock o2) {
		// return o2.mentions.size() - o1.mentions.size();
		// }
		// });

		logger.info("##	" + timestamp + "	##	" + tweetCount);
		for (EntityBlock eblock : ebs.subList(0, 20)) {
			// logger.info(eblock.getTitle() + "			" + eblock.mentions.size()
			// + "			" + eblock.matchingEntities.size() + "	"
			// + eblock.mentions.entrySet());
		}
	}

	public int getEntityBlockSize() {
		return entityBlocks.size();
	}

	public void run() {
		TkET tkET = new TkET(.8, 100, 2, false);
		tkET.setEntityBlocks(new ArrayList<EntityBlock>(entityBlocks.values()));
		List<EntityBlock> topk = null;
		topk = tkET.topk(5);
		if (topk != null) {
			System.out.print("TopKResults:	");
			for (EntityBlock eb : topk)
				System.out.print("(" + (topk.indexOf(eb) + 1) + ")"
						+ eb.getTitle() + " ");
			System.out.println();
		}
	}

}
