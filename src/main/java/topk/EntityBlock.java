package topk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.mongodb.morphia.annotations.Transient;

import com.analog.lyric.dimple.factorfunctions.Sum;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.domains.DiscreteDomain;
import com.analog.lyric.dimple.model.variables.Bit;
import com.analog.lyric.dimple.model.variables.Discrete;
import com.analog.lyric.dimple.model.variables.Variable;
import com.analog.lyric.dimple.options.SolverOptions;
import com.analog.lyric.dimple.solvers.sumproduct.SumProductOptions;
import com.analog.lyric.dimple.solvers.sumproduct.SumProductSolver;

public class EntityBlock {
	private String title;
	
	@Transient
	private transient FactorGraph fg;
	@Transient
	private transient Discrete result;
	
	Map<Integer, Double> CountProbabilities;
	
	@Transient
	Map<Long, Bit> MentionBits;
	
	@Transient
	Map<Long, Boolean> GroundTruthMentionProbabilities;

	int totalCount;

	public EntityBlock(String n) {
		setTitle(n);
		CountProbabilities = new HashMap<Integer, Double>();
		MentionBits = new HashMap<Long, Bit>();
		GroundTruthMentionProbabilities = new HashMap<Long, Boolean>();
	}

	public EntityBlock(String n, String mentions) {
		this(n);
		for (int i = 0; i < mentions.split(",").length; i++) {
			String mentionText = "";
			double p = 0;
			boolean gt = false;

			mentionText = mentions.split(",")[i];
			p = Double.valueOf(mentionText.substring(0,
					mentionText.indexOf('(')));
			gt = (Double.valueOf(mentionText.substring(
					mentionText.indexOf('(') + 1, mentionText.length() - 1)) == 1.0);
			addMention(i, p, gt);
		}

		fg = new FactorGraph();

		int numBits = MentionBits.size();
		Bit[] bits = MentionBits.values().toArray(new Bit[numBits]);
		fg.addVariables(bits);
		result = new Discrete(DiscreteDomain.range(0, numBits));
		result.setName("result");
		Variable[] vs = new Variable[numBits + 1];
		vs[0] = result;
		for (int i = 0; i < numBits; i++)
			vs[i + 1] = bits[i];
		split(fg, vs);

		calculateCountProbabilities();
	}

	public void addMention(long i, double p, boolean gt) {
		MentionBits.put(i, new Bit());
		MentionBits.get(i).setInput(p);
		MentionBits.get(i).setName(getTitle() + "_" + i);
		// int[][] indices = new int[][] { new int[] { 0, 0 }, new int[] {
		// 1, 1
		// } };
		// double[] weights = new double[] { 2.0, 1.0 };
		// FactorTable ft = (FactorTable) FactorTable.create(indices,
		// weights,
		// bits[0], bits[1]);
		// Factor myFactor = fg.addFactor(ft, bits[0], bits[1]);
		// myFactor.setName("TC");
		GroundTruthMentionProbabilities.put(i, gt);

		fg = new FactorGraph();

		int numBits = MentionBits.size();
		Bit[] bits = MentionBits.values().toArray(new Bit[numBits]);

		fg.addVariables(bits);
		result = new Discrete(DiscreteDomain.range(0, numBits));
		result.setName("result");
		Variable[] vs = new Variable[numBits + 1];
		vs[0] = result;
		for (int j = 0; j < numBits; j++)
			vs[j + 1] = bits[j];
		split(fg, vs);

		calculateCountProbabilities();
	}

	public void removeMention(long i) {
		fg.remove(MentionBits.get(i));

		MentionBits.remove(i);
		GroundTruthMentionProbabilities.remove(i);
	}

	public void calculateCountProbabilities() {
		int numBits = MentionBits.size();
		SumProductSolver solver = new SumProductSolver();
		fg.setSolverFactory(solver);
		fg.setOption(SumProductOptions.enableMultithreading, true);
		fg.setOption(SolverOptions.enableMultithreading, true);

		synchronized (fg) {
			fg.solve();
		}

		for (int i = 0; i < numBits + 1; i++)
			if (((double[]) result.getBeliefObject()).length > i)
				CountProbabilities.put(i,
						((double[]) result.getBeliefObject())[i]);
	}

	private static void split(FactorGraph fg, Variable[] vs) {
		int firstHalfSize = (vs.length - 1) / 2;
		int secondHalfSize = vs.length - 1 - firstHalfSize;

		Discrete r1 = new Discrete(DiscreteDomain.range(0, firstHalfSize));
		Discrete r2 = new Discrete(DiscreteDomain.range(0, secondHalfSize));

		Variable[] part1 = new Variable[firstHalfSize + 1];
		Variable[] part2 = new Variable[secondHalfSize + 1];

		part1[0] = r1;
		part2[0] = r2;

		for (int i = 1; i < vs.length; i++) {
			if (i <= firstHalfSize)
				part1[i] = vs[i];
			else
				part2[i - firstHalfSize] = vs[i];
		}

		int minSize = 3;
		if (part1.length < minSize)
			fg.addFactor(new Sum(), part1);
		else
			split(fg, part1);

		if (part2.length < minSize)
			fg.addFactor(new Sum(), part2);
		else
			split(fg, part2);

		Variable[] full = new Variable[3];
		full[0] = vs[0];
		full[1] = r1;
		full[2] = r2;

		fg.addFactor(new Sum(), full);
	}

	public double getDominanceDegree(EntityBlock input) {
		double dp = 0.0;
		for (int i : CountProbabilities.keySet()) {
			double inputSP = 0.0;
			for (int j = 0; j <= i; j++) {
				if (input.CountProbabilities.containsKey(j))
					inputSP += input.CountProbabilities.get(j);
			}
			dp = dp + CountProbabilities.get(i) * inputSP;
		}

		return dp;
	}

	public double getExpectedCountValue() {
		double ev = 0.0;
		for (int c : CountProbabilities.keySet())
			ev = ev + c * CountProbabilities.get(c);
		return ev;
	}

	public HashSet<EntityBlock> getIncoming() {
		HashSet<EntityBlock> incoming = new HashSet<EntityBlock>();
		return incoming;
	}

	public HashSet<EntityBlock> getDomIncoming() {
		HashSet<EntityBlock> domIncoming = new HashSet<EntityBlock>();
		return domIncoming;
	}

	public int getCurrentMentionCount() {
		int c = 0;
		for (Bit m : MentionBits.values())
			if (m.getBelief()[1] >= 1)
				c++;
		return c;
	}

	public boolean isSolid() {
		for (Bit mp : MentionBits.values()) {
			if (!mp.hasFixedValue())
				return false;
		}
		return true;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
