package topk;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperimentOnDataset implements Runnable {
	private static SyntheticDataset syntheticDataset;
	private String fName;
	private int k;
	private double th;
	private int stop;

	Thread t;

	public void run() {
		long beforeExperiment = System.currentTimeMillis();

		String res = "";

		res += fName + "\t" + "exp\t";

		List<EntityBlock> entityBlocks = syntheticDataset.getEntityBlocks();

		TkET tkET = new TkET(th, 100, stop, false);
		tkET.setEntityBlocks(entityBlocks);
		List<EntityBlock> topk = tkET.topk(k);

		List<String> tket_topk = new ArrayList<String>();
		if (topk != null) {
			for (EntityBlock eb : topk)
				tket_topk.add(eb.getTitle());
		}

		Map<String, Integer> tket_gt_rank = new HashMap<String, Integer>();
		for (EntityBlock eb : topk)
			tket_gt_rank.put(eb.getTitle(), syntheticDataset.getGroundTruth()
					.indexOf(eb.getTitle()));

		List<String> gt_topk = new ArrayList<String>();
		for (int i = 0; i < k; i++)
			gt_topk.add(syntheticDataset.getGroundTruth().get(i));

		double l1 = SpearmanFootRule.getL1(tket_topk, gt_topk);
		double l8 = SpearmanFootRule.getL8(tket_topk, gt_topk, tket_gt_rank);

		double saving = ((double) tkET.getTotalMentionCount() - (double) tkET
				.getDisambiguationCount())
				/ (double) tkET.getTotalMentionCount();

		res += tkET.getTotalMentionCount() + "\t"
				+ tkET.getDisambiguationCount() + "\t"
				+ new DecimalFormat("##.##").format(saving) + "\t" + l1 + "\t"
				+ l8 + "\t";

		long experimentTime = System.currentTimeMillis() - beforeExperiment;
		System.out.println("Experimental Time: " + (experimentTime) + " (ms).");
		res += experimentTime + "	";
		res += tket_topk + "	" + tket_gt_rank + "	" + gt_topk + "	";
		try {
			Writer output = new BufferedWriter(new FileWriter(fName + ".tsv",
					true));
			output.write(res);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start(SyntheticDataset sd, int k, double th, double sc) {
		ExperimentOnDataset.syntheticDataset = sd;
		this.k = k;
		this.th = th;
		this.stop = (int) sc;
		fName = "experiment\t"
				+ syntheticDataset.getName().replace("synthetic_dataset", "")
						.replace("_", "\t") + "	" + k + "\t" + th + "\t" + stop
				+ "\t100";
		fName = fName.replace("\t", "_");
		System.out.println("Satarted processing: " + fName);

		run();
		// if (t == null) {
		// t = new Thread(this, fName);
		// t.start();
		// }
	}

}
