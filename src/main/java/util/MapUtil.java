package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MapUtil {
	public static <K, V> List<Entry<K, V>> sortByValue(Map<K, V> input) {
		Set<Entry<K, V>> x = input.entrySet();
		List<Entry<K, V>> y = new ArrayList<Entry<K, V>>();
		y.addAll(x);
		Collections.sort(y, new Comparator<Entry<K, V>>() {
			public int compare(Entry<K, V> arg0, Entry<K, V> arg1) {
				return (String.valueOf(arg1.getValue())).compareTo(String
						.valueOf(arg0.getValue()));
			}
		});

		return y;
	}
}
