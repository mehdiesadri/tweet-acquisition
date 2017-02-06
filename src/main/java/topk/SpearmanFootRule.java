package topk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

public class SpearmanFootRule {

	public static <T> double getL1(List<T> a, List<T> b) {
		double score = 0;
		int k = a.size();

		Set<T> intersection = Sets.intersection(Sets.newHashSet(a),
				Sets.newHashSet(b));
		score = 2 * (k - intersection.size()) * (k + 1);

		for (T item : intersection)
			score += Math.abs(a.indexOf(item) - b.indexOf(item));

		for (T item : a)
			if (!intersection.contains(item))
				score -= a.indexOf(item);

		for (T item : b)
			if (!intersection.contains(item))
				score -= b.indexOf(item);

		// normalization
		// score = score / (.5 * (k ^ 2));
		return score;
	}

	public static <T> double getL8(List<T> a, List<T> gt,
			Map<T, Integer> rank_in_gt) {
		double score = 0;
		int k = a.size();

		Set<T> intersection = Sets.intersection(Sets.newHashSet(a),
				Sets.newHashSet(gt));
		score = 2 * (k - intersection.size()) * (k + 1);

		for (T item : intersection)
			score += Math.abs(a.indexOf(item) - gt.indexOf(item));

		for (T item : a)
			if (!intersection.contains(item))
				score -= Math.abs(a.indexOf(item)
						- (double) rank_in_gt.get(item));

		// normalization
		// score = score / (.5 * (k ^ 2));
		return score;
	}

	public static void main(String[] args) {
		List<String> a = new ArrayList<String>();
		a.add("a");
		a.add("f");
		a.add("d");
		a.add("g");
		a.add("e");
		a.add("c");

		List<String> b = new ArrayList<String>();
		b.add("a");
		b.add("b");
		b.add("c");
		b.add("d");
		b.add("e");
		b.add("f");

		System.out.println("L1 in between " + a + " and " + b + " is "
				+ getL1(a, b));

		Map<String, Integer> rank_in_gt = new HashMap<String, Integer>();
		rank_in_gt.put("a", 0);
		rank_in_gt.put("b", 1);
		rank_in_gt.put("c", 2);
		rank_in_gt.put("d", 3);
		rank_in_gt.put("e", 4);
		rank_in_gt.put("f", 5);
		rank_in_gt.put("g", 9);

		System.out.println("L8 in between " + a + " and " + b + " is "
				+ getL8(a, b, rank_in_gt));
	}

}
