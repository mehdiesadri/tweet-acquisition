package topk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class EntityBlock {

	String title;
	HashSet<String> matchingEntities;
	Map<String, Double> mentions;

	// also factor graph info

	public EntityBlock(String t) {
		matchingEntities = new HashSet<String>();
		mentions = new HashMap<String, Double>();
		title = t;
	}
}
