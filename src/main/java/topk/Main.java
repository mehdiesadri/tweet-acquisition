package topk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

	public static void main(String[] args) throws IOException {
		Map<String, Double> ex_parameters = new HashMap<String, Double>();
		Map<String, Double> dg_parameters = new HashMap<String, Double>();

		dg_parameters.put("windowsize", 1000.0);
		dg_parameters.put("numberofentities", 800.0);
		dg_parameters.put("maxrealsizeparameter", 0.3);
		dg_parameters.put("zipfs", 1.0);

		ex_parameters.put("k", 10.0);
		ex_parameters.put("threshold", .998);
		ex_parameters.put("stoppingcriteria", 1.0);
		ex_parameters.put("edgcapsize", 100.0);

		// RunMotivatingExample();

		boolean x = true;
		while (x) {
			System.out.println("@@@");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			String command = null;

			while (command != "s") {
				if (command != "x")
					System.out.println("Current parameter assignments are: "
							+ ex_parameters);
				System.out
						.println("Please start (s), exit (x) or enter the parameter you want to change (parameter=value): ");
				command = br.readLine().trim();
				if (command.contains("=")) {
					String parameter = command.substring(0,
							command.indexOf("="));
					Double value = Double.valueOf(command.substring(command
							.indexOf("=") + 1));
					ex_parameters.put(parameter, value);
				} else if (command.equals("x")) {
					x = false;
					break;
				} else {
					break;
				}
			}

			if (!x)
				break;

			SyntheticDataset dataset = GenerateDataset(dg_parameters);
			RunExperiment(dataset, ex_parameters);
		}
	}

	private static void RunExperiment(SyntheticDataset dataset,
			Map<String, Double> ex_parameters) throws IOException {
		System.out.println("Experimental Parameters:	" + ex_parameters);

		long beforeExperiment = System.currentTimeMillis();
		int k = ex_parameters.get("k").intValue();
		double th = ex_parameters.get("threshold");
		int stop = ex_parameters.get("stoppingcriteria").intValue();
		int sizeCap = ex_parameters.get("edgcapsize").intValue();

		List<EntityBlock> entityBlocks = dataset.getEntityBlocks();

		TkET tkET = new TkET(th, sizeCap, stop, false);
		tkET.setEntityBlocks(entityBlocks);
		List<EntityBlock> topk = tkET.topk(k);

		if (topk != null) {
			System.out.print("TopKResults:	");
			for (EntityBlock eb : topk)
				System.out.print("(" + (topk.indexOf(eb) + 1) + ")" + eb.getTitle()
						+ " ");
			System.out.println();
		}

		System.out.print("GroundTruth:	");
		for (EntityBlock eb : topk)
			System.out.print("("
					+ (dataset.getGroundTruth().indexOf(eb.getTitle()) + 1) + ")"
					+ eb.getTitle() + " ");
		System.out.println();

		double saving = ((double) tkET.getTotalMentionCount() - (double) tkET
				.getDisambiguationCount())
				/ (double) tkET.getTotalMentionCount();
		System.out.println("Mention Count All: " + tkET.getTotalMentionCount()
				+ " Disambiguated: " + tkET.getDisambiguationCount());
		System.out.println("Saving: "
				+ new DecimalFormat("##.##").format(saving));
		long experimentTime = System.currentTimeMillis() - beforeExperiment;
		System.out.println("Experimental Time: " + (experimentTime) + " (ms).");
	}

	private static SyntheticDataset GenerateDataset(
			Map<String, Double> dg_parameters) throws IOException {
		long beforeSyntheticDataGeneration = System.currentTimeMillis();
		SyntheticDataset syntheticDataset = generateSyntheticDataset(dg_parameters);
		long dataGenerationTime = System.currentTimeMillis()
				- beforeSyntheticDataGeneration;

		System.out.println("########################");
		System.out.println("Data Generation Parameters:	" + dg_parameters);
		System.out.println("Data Generation Time: " + (dataGenerationTime)
				+ " (ms).");
		System.out.println("########################");

		return syntheticDataset;
	}

	private static void RunMotivatingExample() {
		Map<String, Double> ex_parameters = new HashMap<String, Double>();
		Map<String, Double> dg_parameters = new HashMap<String, Double>();

		dg_parameters.put("windowsize", 100000.0);
		dg_parameters.put("numberofentities", 30000.0);
		dg_parameters.put("maxrealsizeparameter", 0.3);
		dg_parameters.put("zipfs", 3.0);

		ex_parameters.put("k", 2.0);
		ex_parameters.put("threshold", .9);
		ex_parameters.put("stoppingcriteria", 1.0);
		ex_parameters.put("edgcapsize", 100.0);

		SyntheticDataset motivating_example = generateMotivatingExampleDataset();

		System.out.println("Experimental Parameters:	" + ex_parameters);
		long beforeExperiment = System.currentTimeMillis();
		int k = ex_parameters.get("k").intValue();
		double th = ex_parameters.get("threshold");
		int stop = ex_parameters.get("stoppingcriteria").intValue();
		int sizeCap = ex_parameters.get("edgcapsize").intValue();

		List<EntityBlock> entityBlocks = motivating_example.getEntityBlocks();

		TkET tkET = new TkET(th, sizeCap, stop, true);
		tkET.setEntityBlocks(entityBlocks);
		List<EntityBlock> topk = tkET.topk(k);

		if (topk != null) {
			System.out.print("TopKResults:	");
			for (EntityBlock eb : topk)
				System.out.print("(" + (topk.indexOf(eb) + 1) + ")" + eb.getTitle()
						+ " ");
			System.out.println();
		}

		System.out.print("GroundTruth:	");
		for (EntityBlock eb : topk)
			System.out
					.print("("
							+ (motivating_example.getGroundTruth().indexOf(
									eb.getTitle()) + 1) + ")" + eb.getTitle() + " ");
		System.out.println();

		double saving = ((double) tkET.getTotalMentionCount() - (double) tkET
				.getDisambiguationCount())
				/ (double) tkET.getTotalMentionCount();
		System.out.println("Mention Count All: " + tkET.getTotalMentionCount()
				+ " Disambiguated: " + tkET.getDisambiguationCount());
		System.out.println("Saving: "
				+ new DecimalFormat("##.##").format(saving));
		// experiment(syntheticDataset, ex_parameters);
		long experimentTime = System.currentTimeMillis() - beforeExperiment;
		System.out.println("Experimental Time: " + (experimentTime) + " (ms).");
	}

	private static SyntheticDataset generateMotivatingExampleDataset() {
		SyntheticDataset motivating_example = new SyntheticDataset();
		List<EntityBlock> entityBlocks = new ArrayList<EntityBlock>();

		entityBlocks.add(new EntityBlock("catfish",
				"0.75(1),0.95(1),0.85(1),0.05(0),0.15(0)"));
		entityBlocks.add(new EntityBlock("magnolia",
				"0.8(1),0.15(0),0.05(0),0.15(0)"));
		entityBlocks.add(new EntityBlock("bad moms", "0.55(1),0.95(1)"));
		entityBlocks.add(new EntityBlock("frozen", "0.05(0),0.95(1)"));
		entityBlocks.add(new EntityBlock("gummo", "0.65(1)"));

		motivating_example.setEntityBlocks(entityBlocks);
		motivating_example.setGroundTruth(Arrays.asList("catfish", "bad moms",
				"frozen", "magnolia", "gummo"));
		return motivating_example;
	}

	private static SyntheticDataset generateSyntheticDataset(
			Map<String, Double> parameters) {
		int n = parameters.get("windowsize").intValue();
		int m = parameters.get("numberofentities").intValue();
		double maxf = parameters.get("maxrealsizeparameter");
		double s = parameters.get("zipfs");
		int maxRankC = 10;
		SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator(n, m,
				maxf, s, maxRankC);
		SyntheticDataset syntheticDataset = sdg.generate();
		return syntheticDataset;
	}
}
