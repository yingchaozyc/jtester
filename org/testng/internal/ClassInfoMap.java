package org.testng.internal;

import org.testng.collections.Maps;
import org.testng.xml.XmlClass;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class和XmlClass的映射关系对照表。
 *
 * @date 2014-5-14 下午9:14:35
 *
 */
@SuppressWarnings("rawtypes")
public class ClassInfoMap {
	private Map<Class<?>, XmlClass> m_map = Maps.newHashMap();
	
	// 嵌套标记
	private boolean includeNestedClasses;

	public ClassInfoMap() {
	}

	/**
	 * 默认初始化include是嵌套的
	 * 
	 * @param classes
	 */
	public ClassInfoMap(List<XmlClass> classes) {
		this(classes, true);
	}


	/**
	 * 遍历XmlClass列表。将每个Class都加载。
	 * 并将对应的Class作为key，原生的XmlClass作为value。
	 * 
	 * @param classes
	 * @param includeNested
	 */
	public ClassInfoMap(List<XmlClass> classes, boolean includeNested) {
		includeNestedClasses = includeNested;
		for (XmlClass xmlClass : classes) {
			try {
				Class c = xmlClass.getSupportClass();
				registerClass(c, xmlClass);
			} catch (NoClassDefFoundError e) {
				Utils.log(
						"[ClassInfoMap]",
						1,
						"Unable to open class " + xmlClass.getName()
								+ " - unable to resolve class reference "
								+ e.getMessage());
				if (xmlClass.loadClasses()) {
					throw e;
				}
			}
		}
	}

	/**
	 * register其实和public void put(Class<?> cls, XmlClass xmlClass)差不多。
	 * 只是多了一部递归嵌套put的过程。
	 * 
	 * @param cl
	 * @param xmlClass
	 */
	private void registerClass(Class cl, XmlClass xmlClass) {
		m_map.put(cl, xmlClass);
		if (includeNestedClasses) {
			// TODO 没有理解这个 getClasses的实现。详见GetClassTest.java.
			for (Class c : cl.getClasses()) {
				if (!m_map.containsKey(c))
					registerClass(c, xmlClass);
			}
		}
	}

	/**
	 * 添加cls映射关系，默认添加的XmlClass为null作为映射关系。
	 * 
	 * @param cls
	 */
	public void addClass(Class<?> cls) {
		m_map.put(cls, null);
	}

	/**
	 * 获取cls的XmlClass映射类。
	 * 
	 * @param cls
	 * @return
	 */
	public XmlClass getXmlClass(Class<?> cls) {
		return m_map.get(cls);
	}

	/**
	 * 添加cls映射关系，添加xmlClass作为映射关系。
	 * 
	 * @param cls
	 */
	public void put(Class<?> cls, XmlClass xmlClass) {
		m_map.put(cls, xmlClass);
	}

	/**
	 * 映射表的键集合。
	 * 
	 * @return
	 */
	public Set<Class<?>> getClasses() {
		return m_map.keySet();
	}

	/**
	 * 返回映射总大小。
	 * 
	 * @return
	 */
	public int getSize() {
		return m_map.size();
	}
}
