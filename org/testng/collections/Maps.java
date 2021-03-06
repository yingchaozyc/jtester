package org.testng.collections;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map生成工具类。
 * 
 * @date 2014-5-16 下午3:38:35
 *
 */
public class Maps {

	public static <K, V> Map<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	public static <K, V> Map<K, V> newHashMap(Map<K, V> parameters) {
		return new HashMap<K, V>(parameters);
	}

	public static <K, V> Map<K, V> newHashtable() {
		return new Hashtable<K, V>();
	}

	public static <K, V> Map<K, V> newLinkedHashMap() {
		return new LinkedHashMap<K, V>();
	}

	public static <K, V> ListMultiMap<K, V> newListMultiMap() {
		return new ListMultiMap<K, V>();
	}

	public static <K, V> SetMultiMap<K, V> newSetMultiMap() {
		return new SetMultiMap<K, V>();
	}
}
