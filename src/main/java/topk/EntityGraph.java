package topk;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class EntityGraph {
	double dominance_threshold;
	double[][] edges;
	List<EntityBlock> ebs;
	boolean print_report;

	public EntityGraph(List<EntityBlock> entityBlocks, double th, boolean pr) {
		print_report = pr;
		dominance_threshold = th;
		ebs = entityBlocks;
		edges = new double[ebs.size()][ebs.size()];
	}

	public double getInDegree(EntityBlock eb) {
		double id = 0.0;
		for (EntityBlock eb_other : ebs) {
			if (eb.equals(eb_other))
				continue;
			double x = edges[ebs.indexOf(eb_other)][ebs.indexOf(eb)];
			double y = edges[ebs.indexOf(eb)][ebs.indexOf(eb_other)];

			// if (x < 1 - dominance_threshold || x > dominance_threshold
			// || y < 1 - dominance_threshold || y > dominance_threshold)
			// continue;
			id += x;
		}
		return id;
	}

	public double getInDegree2(EntityBlock eb) {
		double id = 0.0;
		for (EntityBlock eb_other : ebs) {
			if (eb.equals(eb_other))
				continue;
			double x = edges[ebs.indexOf(eb_other)][ebs.indexOf(eb)];
			// if (x < 1 - dominance_threshold)
			// continue;
			id += x;
		}
		return id;
	}

	public double getInDegree2(EntityBlock eb, List<EntityBlock> topk) {
		double id = 0.0;
		for (EntityBlock eb_other : ebs) {
			if (eb.equals(eb_other) || topk.contains(eb_other))
				continue;
			double x = edges[ebs.indexOf(eb_other)][ebs.indexOf(eb)];
			// if (x < 1 - dominance_threshold)
			// continue;
			id += x;
		}
		return id;
	}

	public double getOutDegree(EntityBlock eb) {
		double od = 0.0;
		for (EntityBlock eb_other : ebs) {
			if (eb.equals(eb_other))
				continue;
			double x = edges[ebs.indexOf(eb)][ebs.indexOf(eb_other)];
			double y = edges[ebs.indexOf(eb_other)][ebs.indexOf(eb)];

			// if (x < 1 - dominance_threshold || x > dominance_threshold
			// || y < 1 - dominance_threshold || y > dominance_threshold)
			// continue;
			od += x;
		}

		return od;
	}

	public double getDomOutDegree(EntityBlock eb) {
		double od = 0.0;
		for (EntityBlock eb_other : ebs) {
			if (eb.equals(eb_other))
				continue;
			int indexOfEB = ebs.indexOf(eb);
			int indexOfOEB = ebs.indexOf(eb_other);
			double x = edges[indexOfEB][indexOfOEB];
			if (x > dominance_threshold)
				od++;
		}

		return od;
	}

	public int disambiguate() {
		EntityBlock eb = selectEntityBlock();
		// EntityBlock eb = selectEntityBlock2();
		if (eb == null)
			return 0;

		long selectedMentionIndex = selectMention(eb);
		double fValue = eb.GroundTruthMentionProbabilities
				.get(selectedMentionIndex) ? 1.0 : 0.0;
		eb.MentionBits.get(selectedMentionIndex).setFixedValue(fValue);
		eb.calculateCountProbabilities();
		if (print_report)
			System.out.println("@@@ " + (fValue == 1.0 ? 1 : -1) + " on "
					+ eb.getTitle());
		return fValue == 1.0 ? 1 : -1;
	}

	private EntityBlock selectEntityBlock() {
		// sort entity blocks based on the dominance degree
		Collections.sort(ebs, new Comparator<EntityBlock>() {
			public int compare(EntityBlock o1, EntityBlock o2) {
				Double d_o1 = getOutDegree(o1) - getInDegree(o1);
				Double d_o2 = getOutDegree(o2) - getInDegree(o2);
				return d_o2.compareTo(d_o1);
			}
		});

		EntityBlock eb = null;
		for (EntityBlock block : ebs) {
			if (!block.isSolid()) {
				eb = block;
				break;
			}
		}
		return eb;
	}

	private EntityBlock selectEntityBlock2() {
		// sort entity blocks based on the dominance degree
		Collections.sort(ebs, new Comparator<EntityBlock>() {
			public int compare(EntityBlock o1, EntityBlock o2) {
				Double d_o1 = getInDegree(o1) - getOutDegree(o1);
				Double d_o2 = getInDegree(o2) - getOutDegree(o2);
				return d_o1.compareTo(d_o2);
			}
		});

		EntityBlock eb = null;
		for (EntityBlock block : ebs) {
			if (!block.isSolid()) {
				eb = block;
				break;
			}
		}
		return eb;
	}

	private long selectMention(EntityBlock eb) {
		if (eb == null)
			return -1;

		// sort <mention,tweet> pairs inside the selected entity block
		Map<Long, Double> opt = new HashMap<Long, Double>();

		double bValue = 0;

		for (Long mentionId : eb.MentionBits.keySet()) {
			if (eb.MentionBits.get(mentionId).hasFixedValue())
				continue;

			bValue = eb.MentionBits.get(mentionId).getBelief()[1];
			if (bValue == 0.0 || bValue == 1.0)
				continue;
			opt.put(mentionId, Math.abs(bValue - .5));
		}

		List<Entry<Long, Double>> opts = new ArrayList<Entry<Long, Double>>(
				opt.entrySet());

		Collections.sort(opts, new Comparator<Entry<Long, Double>>() {
			public int compare(Entry<Long, Double> o1, Entry<Long, Double> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});

		return opts.size() == 0 ? -1 : opts.get(0).getKey();
	}

	// private int selectMention(EntityBlock eb) {
	// if (eb == null)
	// return -1;
	//
	// // sort <mention,tweet> pairs inside the selected entity block
	// Map<Integer, Double> opt = new HashMap<Integer, Double>();
	//
	// double bValue = 0;
	// double pOpt = 0;
	// double nOpt = 0;
	//
	// System.out.println(Arrays.deepToString(edges));
	// for (Integer mentionId : eb.MentionProbabilities.keySet()) {
	// if (eb.MentionProbabilities.get(mentionId).hasFixedValue())
	// continue;
	//
	// bValue = eb.MentionProbabilities.get(mentionId).getBelief()[1];
	// if (bValue == 0.0 || bValue == 1.0)
	// continue;
	//
	// // positive
	// eb.MentionProbabilities.get(mentionId).setInput(1);
	// eb.calculateCountProbabilities();
	// this.updateGraph();
	// pOpt = getOutDegree(eb) - getInDegree(eb);
	// eb.MentionProbabilities.get(mentionId).setInput(bValue);
	//
	// // negative
	// eb.MentionProbabilities.get(mentionId).setInput(0);
	// eb.calculateCountProbabilities();
	// this.updateGraph();
	// nOpt = getOutDegree(eb) - getInDegree(eb);
	// eb.MentionProbabilities.get(mentionId).setInput(bValue);
	//
	// opt.put(mentionId, pOpt * bValue + nOpt * (1 - bValue));
	//
	// // back to normal
	// eb.calculateCountProbabilities();
	// this.updateGraph();
	// }
	// System.out.println(Arrays.deepToString(edges));
	//
	// List<Entry<Integer, Double>> opts = new ArrayList<Entry<Integer,
	// Double>>(
	// opt.entrySet());
	//
	// Collections.sort(opts, new Comparator<Entry<Integer, Double>>() {
	// @Override
	// public int compare(Entry<Integer, Double> o1,
	// Entry<Integer, Double> o2) {
	// return o2.getValue().compareTo(o1.getValue());
	// }
	// });
	//
	// return opts.size() == 0 ? -1 : opts.get(0).getKey();
	// }

	public void printReport() {
		List<EntityBlock> entityBlocks = new ArrayList<EntityBlock>(ebs);
		System.out
				.println("$$$$$  Printing the Entity Graph's Report...  $$$$$");
		for (EntityBlock eb : entityBlocks) {
			System.out.print("%%% " + eb.getTitle() + " %% ");

			for (int cp : eb.CountProbabilities.keySet()) {
				System.out.print(cp
						+ ":("
						+ new DecimalFormat("##.###")
								.format(eb.CountProbabilities.get(cp)) + ") ");
			}

			System.out.print(" %% EV: "
					+ new DecimalFormat("##.###").format(eb
							.getExpectedCountValue()));

			System.out.println(" %% IN,Out,Full Degree: "
					+ new DecimalFormat("##.###").format(getInDegree(eb))
					+ ", "
					+ new DecimalFormat("##.###").format(getOutDegree((eb)))
					+ ", "
					+ new DecimalFormat("##.###").format(getOutDegree(eb)
							- getInDegree(eb)));
		}

		System.out
				.println("$$$$$  Done Printing the Entity Graph's Report!  $$$$$");
	}

	public void printGraph() {
		System.out.println("$$$$$  Printing the Entity Graph...  $$$$$");
		System.out
				.println("graph[rankdir=LR, center=true, margin=0.1, nodesep=0.1, ranksep=0.4]");
		System.out
				.println("node[shape=circle, fontname=\"Courier-Bold\", fontsize=10, width=1, height=0.5, fixedsize=true]");
		System.out.println("edge[arrowsize=0.6, arrowhead=vee]");

		for (int i = 0; i < ebs.size(); i++) {
			for (int j = 0; j < ebs.size(); j++) {
				if (i != j) {
					double d = edges[i][j];
					if (d >= dominance_threshold
							|| d <= 1 - dominance_threshold) {
						System.out.println(ebs.get(i).getTitle()
								.replace(" ", "_")
								+ " -> "
								+ ebs.get(j).getTitle().replace(" ", "_")
								+ "[label="
								+ new DecimalFormat("##.##").format(d) + "];");
					} else
						System.out.println(ebs.get(i).getTitle()
								.replace(" ", "_")
								+ " -> "
								+ ebs.get(j).getTitle().replace(" ", "_")
								+ "[style=\"dashed\", label="
								+ new DecimalFormat("##.##").format(d) + "];");
				}
			}
		}

		System.out.println("$$$  Done Printing the Entity Graph!  $$$");
	}

	public void updateGraph() {
		edges = new double[ebs.size()][ebs.size()];
		for (int i = 0; i < ebs.size(); i++) {
			for (int j = 0; j < ebs.size(); j++) {
				if (i != j)
					edges[i][j] = ebs.get(i).getDominanceDegree(ebs.get(j));
			}
		}
	}

	public int getTotalMentionCount() {
		int count = 0;
		for (EntityBlock eb : ebs)
			count += eb.MentionBits.size();
		return count;
	}

	public List<EntityBlock> checkStoppingCriteria(int k,
			boolean disambiguationOver) {
		List<EntityBlock> topk = new ArrayList<EntityBlock>();
		Collections.sort(ebs, new Comparator<EntityBlock>() {
			public int compare(EntityBlock a, EntityBlock b) {
				Double aDomOutDegree = getDomOutDegree(a);
				Double bDomOutDegree = getDomOutDegree(b);
				return bDomOutDegree.compareTo(aDomOutDegree);
			}
		});

		for (EntityBlock eb_n : ebs) {
			// System.out.println(Arrays.deepToString(edges));
			Double domOutDegree = getDomOutDegree(eb_n, topk);
			double d = (double) ebs.size() - (double) k;
			if (domOutDegree >= d)
				topk.add(eb_n);
			if (topk.size() == k)
				return topk;
		}

		if (disambiguationOver) {
			for (EntityBlock eb : ebs) {
				if (!topk.contains(eb))
					topk.add(eb);
				if (topk.size() >= k)
					return topk;
			}
		}

		return null;
	}

	public List<EntityBlock> checkStoppingCriteria2(int k,
			boolean disambiguationOver) {
		List<EntityBlock> topk = new ArrayList<EntityBlock>();
		Collections.sort(ebs, new Comparator<EntityBlock>() {
			public int compare(EntityBlock a, EntityBlock b) {
				Double aInDegree = getInDegree2(a);
				Double bInDegree = getInDegree2(b);
				return aInDegree.compareTo(bInDegree);
			}
		});

		for (EntityBlock eb_n : ebs) {
			// System.out.println(Arrays.deepToString(edges));
			Double inDegree = getInDegree2(eb_n, topk);
			if (inDegree == 0)
				topk.add(eb_n);
			if (topk.size() == k)
				return topk;
		}

		if (disambiguationOver) {
			for (EntityBlock eb : ebs) {
				if (!topk.contains(eb))
					topk.add(eb);
				if (topk.size() >= k)
					return topk;
			}
		}

		return null;
	}

	private Double getDomOutDegree(EntityBlock eb, List<EntityBlock> topk) {
		double od = 0.0;
		for (EntityBlock eb_other : ebs) {
			if (eb.equals(eb_other) || topk.contains(eb_other))
				continue;
			int indexOfEB = ebs.indexOf(eb);
			int indexOfOEB = ebs.indexOf(eb_other);
			double x = edges[indexOfEB][indexOfOEB];
			if (x > dominance_threshold)
				od++;
		}

		return od;
	}
}
