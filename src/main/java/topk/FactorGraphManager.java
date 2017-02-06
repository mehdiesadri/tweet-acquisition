package topk;

import java.io.IOException;
import java.util.Random;

import com.analog.lyric.dimple.factorfunctions.Sum;
import com.analog.lyric.dimple.factorfunctions.core.FactorTable;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.domains.DiscreteDomain;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Bit;
import com.analog.lyric.dimple.model.variables.Discrete;
import com.analog.lyric.dimple.model.variables.Variable;
import com.analog.lyric.dimple.options.SolverOptions;
import com.analog.lyric.dimple.solvers.sumproduct.SumProductSolver;

public class FactorGraphManager {
	public static void main(String[] args) throws IOException {
		long start = System.currentTimeMillis();

		// Random rand = new Random();
		FactorGraph fg = new FactorGraph();

		// int numBits = 1000;
		// Bit[] bits = new Bit[numBits];
		// for (int i = 0; i < numBits; i++) {
		// Bit x = new Bit();
		// x.setName("x" + i);
		// x.setInput(0.5);
		// // a.setFixedValue(1); // after disambiguation
		// bits[i] = x;
		// }

		int numBits = 5;
		Bit[] bits = new Bit[numBits];
		bits[0] = new Bit();
		bits[0].setName("t2");
		bits[0].setInput(0.95);

		bits[1] = new Bit();
		bits[1].setName("t4");
		bits[1].setInput(0.85);

		bits[2] = new Bit();
		bits[2].setName("t9");
		bits[2].setInput(0.95);

		bits[3] = new Bit();
		bits[3].setName("t5");
		bits[3].setInput(0.5);

		bits[4] = new Bit();
		bits[4].setName("t11");
		bits[4].setInput(0.15);

		fg.addVariables(bits);

		// int[][] indices = new int[][] { new int[] { 0, 0 }, new int[] { 1, 1
		// } };
		// double[] weights = new double[] { 2.0, 1.0 };
		// FactorTable ft = (FactorTable) FactorTable.create(indices, weights,
		// bits[0], bits[1]);
		// Factor myFactor = fg.addFactor(ft, bits[0], bits[1]);
		// myFactor.setName("TC");

		// for (int i = 0; i < numBits; i++) {
		// int[][] indices = new int[][] { new int[] { 0, 0 },
		// new int[] { 1, 1 } };
		// double[] weights = new double[] { 2.0, 1.0 };
		// int opponent = -1;
		// while (opponent == -1 || opponent == i)
		// opponent = rand.nextInt(numBits);
		// FactorTable ft = (FactorTable) FactorTable.create(indices, weights,
		// bits[i], bits[opponent]);
		// Factor myFactor = fg.addFactor(ft, bits[i], bits[opponent]);
		// // myFactor.setName("MyFactor");
		// }

		Discrete result = new Discrete(DiscreteDomain.range(0, numBits));
		result.setName("result");

		Variable[] vs = new Variable[numBits + 1];
		vs[0] = result;
		for (int i = 0; i < numBits; i++)
			vs[i + 1] = bits[i];

		split(fg, vs);

		fg.setSolverFactory(new SumProductSolver());
		fg.setOption(SolverOptions.enableMultithreading, true);

		bits[4].setInput(0.5);
		fg.solve();
		bits[4].setInput(0.15);
		fg.solve();

		System.out.println(bits[4].getValue());
		System.out.println(bits[4].getBelief()[0]);
		System.out.println(bits[4].getBelief()[1]);

		// for (Variable x : fg.getVariables()) {
		// if (x.getName() != "result")
		// System.out.println(x.getName() + ": "
		// + ((double[]) x.getBeliefObject())[0]);
		// }

		for (int i = 0; i < numBits + 1; i++)
			System.out.println(result.getName() + "[" + i + "]: "
					+ ((double[]) result.getBeliefObject())[i]);

		System.out.println("Time: " + (System.currentTimeMillis() - start)
				+ "ms");
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

		Factor sum = fg.addFactor(new Sum(), full);
	}
}
