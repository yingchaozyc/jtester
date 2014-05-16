package org.testng.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * ListMultiMap是这样一个对象。本质仍然是一个Map。
 * 
 * key是任意的类型，但是value是一个List。
 * 如果准备put的key不存在，创建一个新的List作为value插入。
 * 如果key存在，List中add指定的新元素，更新key对应的value为当前List。
 * 
 * 这个集合可以被用在实际项目中的。
 * 
 * A container to hold lists indexed by a key.
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class ListMultiMap<K, V> {
	private Map<K, List<V>> m_objects = Maps.newHashMap();

	public void put(K key, V method) {
		List<V> l = m_objects.get(key);
		if (l == null) {
			l = Lists.newArrayList();
			m_objects.put(key, l);
		}
		l.add(method);
	}
	
	public static void main(String[] args) {
		ListMultiMap<String, String> listMap = new ListMultiMap<String, String>();
		listMap.put("1", "one");
		
		System.out.println(listMap);
	}
	
	public List<V> get(K key) {
		return m_objects.get(key);
	} 
	 
	public List<K> getKeys() {
		return new ArrayList(m_objects.keySet()); 
	}

	public boolean containsKey(K k) {
		return m_objects.containsKey(k);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		List<K> indices = getKeys();
		// Collections.sort(indices);
		for (K i : indices) {
			result.append("\n    ").append(i).append(" <-- ");
			for (Object o : m_objects.get(i)) {
				result.append(o).append(" ");
			}
		}
		return result.toString();
	}

	public boolean isEmpty() {
		return m_objects.size() == 0;
	}

	public int getSize() {
		return m_objects.size();
	}

	public List<V> remove(K key) {
		return m_objects.remove(key);
	}

	public Set<Entry<K, List<V>>> getEntrySet() {
		return m_objects.entrySet();
	}

	public Collection<List<V>> getValues() {
		return m_objects.values();
	}

	public void putAll(K k, Collection<V> values) {
		for (V v : values) {
			put(k, v);
		}
	}

	/**
	 * 和new一个对象含义相同。
	 * 
	 * @return
	 */
	public static <K, V> ListMultiMap<K, V> create() {
		return new ListMultiMap<K, V>();
	}
}
