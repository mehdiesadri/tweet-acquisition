package topk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class Experiment {
	static List<String> experiments;

	public static void main(String[] args) throws InterruptedException,
			IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String command = null;

		System.out.println("Generate Datasets?");
		command = br.readLine().trim();
		if (command.contains("y") || command.contains("1"))
			generateDatasets();

		System.out.println("Extract Synthetic Dataset Distributions?");
		command = br.readLine().trim();
		if (command.contains("y") || command.contains("1"))
			extractSyntheticDatasetDistributions();

		System.out.println("Start Experiments?");
		command = br.readLine().trim();
		if (command.contains("y") || command.contains("1"))
			conductExperiments();

		System.out.println("Aggregate Results?");
		command = br.readLine().trim();
		if (command.contains("y") || command.contains("1"))
			aggregateResults();
	}

	private static void aggregateResults() throws IOException {
		experiments = new ArrayList<String>();
		experiments
				.add("n\tm\tmaxf\tzipfs\tk\tth\tstop\tedg_cap\ttotal_mention_count\tdisambiguated_mention_count\tsaving\tL1\tL8\ttime");
		String rootPath = ".";

		File folder = new File(rootPath);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			String fName = file.getName();
			if (file.isFile() && fName.contains("experiment__")) {
				FileInputStream fstream = new FileInputStream(file);

				String line = new BufferedReader(new InputStreamReader(fstream))
						.readLine();

				int indexOfExp = line.indexOf("\texp");
				String line1 = line.substring(0, indexOfExp);
				line1 = line1.replace("_", "\t");
				line = line1 + line.substring(indexOfExp);

				String[] line_parts = line.split("\t");

				int n = Integer.valueOf(line_parts[2].trim());
				int m = Integer.valueOf(line_parts[3].trim());
				double maxf = Double.valueOf(line_parts[4].trim());
				double zipfs = Double.valueOf(line_parts[5].trim());
				int k = Integer.valueOf(line_parts[6].trim());
				double th = Double.valueOf(line_parts[7].trim());
				int stop = Integer.valueOf(line_parts[8].trim());
				int edg_cap = Integer.valueOf(line_parts[9].trim());

				int total_mention_count = Integer
						.valueOf(line_parts[11].trim());
				int disambiguated_mention_count = Integer
						.valueOf(line_parts[12].trim());
				double saving = Double.valueOf(line_parts[13].trim());
				double l1 = Double.valueOf(line_parts[14].trim());
				double l8 = Double.valueOf(line_parts[15].trim());
				long time = Long.valueOf(line_parts[16].trim());

				experiments.add(n + "\t" + m + "\t" + maxf + "\t" + zipfs
						+ "\t" + k + "\t" + th + "\t" + stop + "\t" + edg_cap
						+ "\t" + total_mention_count + "\t"
						+ disambiguated_mention_count + "\t" + saving + "\t"
						+ l1 + "\t" + l8 + "\t" + time);

				fstream.close();
			}
		}

		File fout = new File("all_experiments.tsv");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fout);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			for (String e : experiments) {
				bw.write(e);
				bw.newLine();
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void conductExperiments() throws InterruptedException,
			IOException {
		SyntheticDataset syntheticDataset = new SyntheticDataset();
		String rootPath = ".";

		File folder = new File(rootPath);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			String fName = file.getName();
			if (file.isFile() && fName.startsWith("synthetic")) {
				List<EntityBlock> entityBlocks = new ArrayList<EntityBlock>();
				List<String> groundTruth = new ArrayList<String>();

				// Open the file
				FileInputStream fstream = new FileInputStream(file);
				BufferedReader br = new BufferedReader(new InputStreamReader(
						fstream));

				String line;

				// Read File Line By Line
				while ((line = br.readLine()) != null) {
					if (line.startsWith("e_")) {
						String[] parts = line.split("_");
						String name = parts[0].trim() + "_" + parts[1].trim();
						String mentions = parts[2].trim();
						entityBlocks.add(new EntityBlock(name, mentions));
					} else if (line.startsWith("GT")) {
						String[] es = line.substring(5, line.length() - 1)
								.split(",");
						for (String e : es)
							groundTruth.add(e.trim());
					}
				}

				// Close the input stream
				br.close();

				syntheticDataset.setEntityBlocks(entityBlocks);
				syntheticDataset.setGroundTruth(groundTruth);
				syntheticDataset.setName(fName);

				int[] ks = { 2, 5, 10, 15, 25, 50, 100 };
				double[] ths = { .51, .55, .6, .7, .8, .9, .98, .99, 1 };
				double[] scs = { 1.0, 2.0 };

				for (int k : ks) {
					if ((double) k >= ((double) syntheticDataset
							.getEntityBlocks().size() / 2))
						continue;

					for (double th : ths) {
						for (double sc : scs) {
							synchronized (syntheticDataset) {
								ExperimentOnDataset experiment = new ExperimentOnDataset();
								experiment.start(syntheticDataset, k, th, sc);
							}
						}
						Thread.sleep(100);
					}
					Thread.sleep(1000);
				}
			}
		}
	}

	private static void extractSyntheticDatasetDistributions()
			throws InterruptedException, IOException {
		String rootPath = ".";

		File folder = new File(rootPath);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			String fName = file.getName();
			if (file.isFile() && fName.startsWith("synthetic")) {
				List<EntityBlock> entityBlocks = new ArrayList<EntityBlock>();
				List<String> groundTruth = new ArrayList<String>();

				// Open the file
				FileInputStream fstream = new FileInputStream(file);
				BufferedReader br = new BufferedReader(new InputStreamReader(
						fstream));

				String line;

				// Read File Line By Line
				while ((line = br.readLine()) != null) {
					if (line.startsWith("e_")) {
						String[] parts = line.split("_");
						String name = parts[0].trim() + "_" + parts[1].trim();
						String mentions = parts[2].trim();
						entityBlocks.add(new EntityBlock(name, mentions));
					} else if (line.startsWith("GT")) {
						String[] es = line.substring(5, line.length() - 1)
								.split(",");
						for (String e : es)
							groundTruth.add(e.trim());
					}
				}

				// Close the input stream
				br.close();

				File fout = new File("dist_" + fName + ".tsv");
				FileOutputStream fos;
				try {
					fos = new FileOutputStream(fout);
					BufferedWriter bw = new BufferedWriter(
							new OutputStreamWriter(fos));

					bw.write("name\tsize\trealsize");
					bw.newLine();

					for (EntityBlock eb : entityBlocks) {
						int real_size = 0;
						for (long x : eb.GroundTruthMentionProbabilities
								.keySet())
							if (eb.GroundTruthMentionProbabilities.get(x))
								real_size++;
						bw.write(eb.getTitle() + "\t" + eb.MentionBits.size()
								+ "\t" + real_size);
						bw.newLine();
					}

					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void generateDatasets() throws InterruptedException {
		int[] wss = { 100000 };
		double[] nes = { .4 };
		double[] mrsps = { 0.7 };
		double[] ss = { 4 };

		for (int ws : wss) {
			for (double nep : nes) {
				for (double mrsp : mrsps) {
					for (double s : ss) {
						int ne = (int) (nep * ws);
						SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator(
								ws, ne, mrsp, s, 10);
						sdg.start();
					}
				}
			}
		}
	}
}
