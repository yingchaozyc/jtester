package org.testng.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * TestNG自身的Set工具类。或许叫HashSet工具类更合适。
 * 
 * @date 2014-5-16 下午3:30:10
 *
 */
public class Sets {

	public static <V> Set<V> newHashSet() {
		return new HashSet<V>();
	}

	public static <V> Set<V> newHashSet(Collection<V> c) {
		return new HashSet<V>(c);
	}
}
