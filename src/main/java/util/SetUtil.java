package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public class SetUtil {
	public static List<Set<String>> getSubsets(HashSet<String> terms, int k) {
		List<Set<String>> res = new ArrayList<Set<String>>();
		getSubsets(terms, k, 0, new HashSet<String>(), res);
		return res;
	}

	public static String[] ListToArray(ArrayList<String> input) {
		String[] arr = new String[input.size()];
		for (int i = 0; i < input.size(); i++) {
			arr[i] = input.get(i);
		}
		return arr;
	}

	public static void main(String[] args) throws Exception {
		HashMap<String, Double> test = new HashMap<String, Double>();
		test.put("a", (double) 2);
		test.put("b", (double) 1);
		test.put("c", (double) 3);

		System.out.println(test);
		System.out.println(sort_asc(test));
		System.out.println(sort_desc(test));

		Set<Integer> mySet = new HashSet<Integer>();

		for (int i = 1; i <= 5; i++)
			mySet.add(i);

		Set<Set<Integer>> powSet = powerSet(mySet, 3);

		for (Set<Integer> s : powSet) {
			System.out.println(s);
		}
	}

	public static <T> Set<Set<T>> powerSet(Set<T> originalSet, int n) {
		Set<Set<T>> sets = new HashSet<Set<T>>();
		if (originalSet.isEmpty() || n == 0) {
			sets.add(new HashSet<T>());
			return sets;
		}

		List<T> list = new ArrayList<T>(originalSet);
		T head = list.get(0);
		Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
		Set<Set<T>> ps = powerSet(rest, n);
		for (Set<T> set : ps) {
			Set<T> newSet = new TreeSet<T>();
			newSet.add(head);
			newSet.addAll(set);
			if (newSet.size() <= n)
				sets.add(newSet);
			if (set.size() <= n)
				sets.add(set);
		}

		return sets;
	}

	public static List<Entry<String, Double>> sort_asc(
			Map<String, Double> clTerms) {
		List<Entry<String, Double>> entries = new ArrayList<Entry<String, Double>>();
		entries.addAll(clTerms.entrySet());
		Collections.sort(entries, new Comparator<Entry<String, Double>>() {
			public int compare(Entry<String, Double> o1,
					Entry<String, Double> o2) {
				return (int) (o1.getValue() - o2.getValue());
			}
		});

		return entries;
	}

	public static List<Entry<String, Double>> sort_desc(
			Map<String, Double> input) {
		List<Entry<String, Double>> entries = new ArrayList<Entry<String, Double>>();
		if (input != null && input.size() > 0) {
			entries.addAll(input.entrySet());
			Collections.sort(entries, new Comparator<Entry<String, Double>>() {
				public int compare(Entry<String, Double> o1,
						Entry<String, Double> o2) {
					return (int) (o2.getValue() - o1.getValue());
				}
			});
		}

		return entries;
	}

	private static void getSubsets(HashSet<String> terms, int k, int idx,
			Set<String> current, List<Set<String>> solution) {
		if (current.size() == k) {
			solution.add(new HashSet<String>(current));
			return;
		}

		if (terms == null || idx == terms.size())
			return;

		String x = (String) terms.toArray()[idx];
		current.add(x);
		getSubsets(terms, k, idx + 1, current, solution);
		current.remove(x);
		getSubsets(terms, k, idx + 1, current, solution);
	}

}
