package org.testng.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * ArrayList生成工具类。 
 *
 * @date 2014-5-16 下午3:36:01
 *
 */
public class Lists {

	public static <K> List<K> newArrayList() {
		return new ArrayList<K>();
	}

	public static <K> List<K> newArrayList(Collection<K> c) {
		return new ArrayList<K>(c);
	}

	public static <K> List<K> newArrayList(K... elements) {
		List<K> result = new ArrayList<K>();
		for (K e : elements) {
			result.add(e);
		}
		return result;
	}

	public static <K> List<K> newArrayList(int size) {
		return new ArrayList<K>(size);
	}
}
