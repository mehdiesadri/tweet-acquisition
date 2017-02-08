package topk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TkET {
	private double dominance_threshold;
	private int edgSizeCap;
	private List<EntityBlock> entityBlocks;
	private Map<List<EntityBlock>, EntityGraph> subgraphs;

	private int totalMentionCount;
	private int disambiguationCount;
	private int stopping_criteria;
	private boolean print_report;

	public TkET(double th, int sizeCap, int stop, boolean pr) {
		stopping_criteria = stop;
		edgSizeCap = sizeCap;
		this.print_report = pr;
		totalMentionCount = 0;
		disambiguationCount = 0;
		dominance_threshold = th;
		entityBlocks = new ArrayList<EntityBlock>();
		subgraphs = new HashMap<List<EntityBlock>, EntityGraph>();
	}

	public List<EntityBlock> getEntityBlocks() {
		return entityBlocks;
	}

	public void setEntityBlocks(List<EntityBlock> entityBlocks) {
		this.entityBlocks = entityBlocks;
		List<List<EntityBlock>> partitions = split(entityBlocks);
		for (List<EntityBlock> partition : partitions) {
			EntityGraph eg = new EntityGraph(partition,
					partitions.size() > 1 ? Math.sqrt(dominance_threshold)
							: dominance_threshold, print_report);
			subgraphs.put(partition, eg);
			setTotalMentionCount(getTotalMentionCount()
					+ eg.getTotalMentionCount());
		}
	}

	public List<EntityBlock> topk(int k) {
		List<EntityBlock> globalTopk = null;
		List<EntityBlock> topks = new ArrayList<EntityBlock>();

		for (EntityGraph eg : subgraphs.values()) {
			List<EntityBlock> localTopk = topk(eg, k);
			if (localTopk != null)
				topks.addAll(localTopk);
			if (subgraphs.size() == 1)
				return topks;
		}

		EntityGraph meta_eg = new EntityGraph(topks, dominance_threshold,
				print_report);
		globalTopk = topk(meta_eg, k);
		return globalTopk;
	}

	public List<EntityBlock> topk(EntityGraph eg, int k) {
		List<EntityBlock> topk = null;
		for (EntityBlock eb : eg.ebs)
			eb.calculateCountProbabilities();
		
		eg.updateGraph();
		
		if (print_report)
			eg.printGraph();
		
		if (stopping_criteria == 1)
			topk = eg.checkStoppingCriteria(k, false);
		else
			topk = eg.checkStoppingCriteria2(k, false);

		while (topk == null) {
			int x = eg.disambiguate();
			eg.updateGraph();
			setDisambiguationCount(getDisambiguationCount() + 1);
			if (print_report)
				eg.printGraph();
			if (x == 0) {
				if (stopping_criteria == 1)
					topk = eg.checkStoppingCriteria(k, true);
				else
					topk = eg.checkStoppingCriteria2(k, true);
				setDisambiguationCount(getDisambiguationCount() - 1);
				break;
			}
			if (stopping_criteria == 1)
				topk = eg.checkStoppingCriteria(k, false);
			else
				topk = eg.checkStoppingCriteria2(k, false);
		}
		return topk;
	}

	public List<List<EntityBlock>> split(List<EntityBlock> input) {
		List<List<EntityBlock>> output = new ArrayList<List<EntityBlock>>();
		List<EntityBlock> currentList = new ArrayList<EntityBlock>();

		for (EntityBlock eb : input) {
			if (currentList.size() < edgSizeCap)
				currentList.add(eb);
			else {
				output.add(currentList);
				currentList = new ArrayList<EntityBlock>();
				currentList.add(eb);
			}
		}

		output.add(currentList);
		return output;
	}

	public int getTotalMentionCount() {
		return totalMentionCount;
	}

	public void setTotalMentionCount(int totalMentionCount) {
		this.totalMentionCount = totalMentionCount;
	}

	public int getDisambiguationCount() {
		return disambiguationCount;
	}

	public void setDisambiguationCount(int disambiguationCount) {
		this.disambiguationCount = disambiguationCount;
	}
}
