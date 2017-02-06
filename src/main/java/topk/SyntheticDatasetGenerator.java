package topk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class SyntheticDatasetGenerator implements Runnable {
	private int n;
	private int m;
	private double max_f;
	private double zipfs;
	private double max_rank_coefficient;

	public Thread t;

	Map<Integer, Map<Integer, Double>> ebs;
	Map<Integer, Map<Integer, Boolean>> ebs_gt;
	Map<Integer, Integer> ebs_size;
	Map<Integer, Integer> ebs_real_size;

	private static Random rand;
	private static ZipfGenerator zipf;
	private List<String> entityBlockStrings;
	private List<String> ground_truth;

	private SyntheticDataset syntheticDataset;

	public SyntheticDatasetGenerator(int n, int m, double maxf, double s,
			double max_rank_coefficient) {
		this.n = n;
		this.m = m;
		this.max_f = maxf;
		this.zipfs = s;
		this.max_rank_coefficient = max_rank_coefficient;

		this.ebs = new HashMap<Integer, Map<Integer, Double>>();
		this.ebs_gt = new HashMap<Integer, Map<Integer, Boolean>>();
		this.ebs_size = new HashMap<Integer, Integer>();
		this.ebs_real_size = new HashMap<Integer, Integer>();

		rand = new Random();
	}

	public SyntheticDataset generate() {
		syntheticDataset = new SyntheticDataset();
		zipf = new ZipfGenerator(
				(int) ((double) m * (double) max_rank_coefficient), zipfs);

		int nextRank = 0;
		int total = 0;
		// int newTotal = 0;

		// randomize sizes
		for (int i = 1; i < m + 1; i++) {
			nextRank = zipf.next();
			while (nextRank == 0)
				nextRank = zipf.next();
			ebs_size.put(i, nextRank);
			total += nextRank;
		}

		// normalize sizes
		for (int i = 1; i < m + 1; i++) {
			int size = (int) Math
					.ceil(((double) ebs_size.get(i) / (double) total)
							* (double) n);
			ebs_size.put(i, size);
			// newTotal += size;
		}

		// randomize and add tweets
		for (int i = 1; i < m + 1; i++) {
			double rnd = rand.nextDouble();
			while (rnd > max_f)
				rnd = rand.nextDouble();
			int realSize = (int) Math.pow(ebs_size.get(i), (double) 1 - rnd);
			ebs_real_size.put(i, realSize);

			Map<Integer, Double> tweets = new HashMap<Integer, Double>();
			for (int j = 1; j < ebs_size.get(i) + 1; j++) {
				rnd = rand.nextDouble();
				while (rnd < .45)
					rnd = rand.nextDouble();
				tweets.put(j, rnd);
			}

			List<Entry<Integer, Double>> tweets_to_sort = new ArrayList<Map.Entry<Integer, Double>>(
					tweets.entrySet());
			Collections.sort(tweets_to_sort,
					new Comparator<Entry<Integer, Double>>() {
						public int compare(Entry<Integer, Double> a,
								Entry<Integer, Double> b) {
							return b.getValue().compareTo(a.getValue());
						}
					});

			Map<Integer, Boolean> gt = new HashMap<Integer, Boolean>();
			for (Entry<Integer, Double> t : tweets_to_sort) {
				if (realSize > 0) {
					gt.put(t.getKey(), true);
					realSize--;
				} else
					gt.put(t.getKey(), false);
			}

			ebs.put(i, tweets);
			ebs_gt.put(i, gt);
		}

		List<EntityBlock> entityBlocks = new ArrayList<EntityBlock>();
		entityBlockStrings = new ArrayList<String>();

		for (int e_id : ebs.keySet()) {
			String mentions = "";
			for (int t_id : ebs.get(e_id).keySet()) {
				Boolean t_gt = ebs_gt.get(e_id).get(t_id);
				Double t_p = ebs.get(e_id).get(t_id);
				String t = t_p + "(" + (t_gt ? "1" : "0") + ")";
				mentions += t + ",";
			}

			mentions = mentions.substring(0, mentions.length() - 1);
			entityBlocks.add(new EntityBlock("e_" + e_id, mentions));
			entityBlockStrings.add("e_" + e_id + "_" + mentions);
		}

		ground_truth = new ArrayList<String>();
		List<Entry<Integer, Integer>> entities_to_sort = new ArrayList<Map.Entry<Integer, Integer>>(
				ebs_real_size.entrySet());
		Collections.sort(entities_to_sort,
				new Comparator<Entry<Integer, Integer>>() {
					public int compare(Entry<Integer, Integer> a,
							Entry<Integer, Integer> b) {
						return b.getValue().compareTo(a.getValue());
					}
				});

		for (Entry<Integer, Integer> e : entities_to_sort)
			ground_truth.add("e_" + e.getKey());

		syntheticDataset.setGroundTruth(ground_truth);
		// System.out.println(newTotal);
		// System.out.println(ebs_size);
		// System.out.println(ebs_real_size);
		// System.out.println(ebs);

		syntheticDataset.setEntityBlocks(entityBlocks);
		return syntheticDataset;
	}

	public void run() {
		String fName = "synthetic_dataset_" + n + "_" + m + "_" + max_f + "_"
				+ zipfs;
		syntheticDataset = generate();
		// save the object to file
		File fout = new File(fName);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			for (String eb : entityBlockStrings) {
				bw.write(eb);
				bw.newLine();
			}

			bw.write("GT: " + ground_truth.toString());
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void start() {
		String fName = "synthetic_dataset_" + n + "_" + m + "_" + max_f + "_"
				+ zipfs;
		System.out.println("Starting to generate: " + fName);
		if (t == null) {
			t = new Thread(this, fName);
			t.start();
		}
	}
}
