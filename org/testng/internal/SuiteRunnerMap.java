package org.testng.internal;

import org.testng.ISuite;
import org.testng.TestNGException;
import org.testng.collections.Maps;
import org.testng.xml.XmlSuite;

import java.util.Collection;
import java.util.Map;

/**
 * SuiteRunner映射集合。key: xmlSuite.getName()  value: SuiteRunner(ISuite)
 * 
 * @date 2014-5-14 下午2:16:48
 *
 */
public class SuiteRunnerMap {

	private Map<String, ISuite> m_map = Maps.newHashMap();

	/**
	 * 扔一个SuiteRunner到map。有重复的名称抛出TestNGException。
	 * 
	 * @param xmlSuite
	 * @param suite
	 */
	public void put(XmlSuite xmlSuite, ISuite suite) {
		final String name = xmlSuite.getName();
		if (m_map.containsKey(name)) {
			throw new TestNGException(
					"SuiteRunnerMap already have runner for suite " + name);
		}
		m_map.put(name, suite);
	}

	/**
	 * 根据Suite的名字取获取SuiteRunner。
	 * 
	 * @param xmlSuite
	 * @return
	 */
	public ISuite get(XmlSuite xmlSuite) {
		return m_map.get(xmlSuite.getName());
	}

	/**
	 * 获取SuiteRunner列表。
	 * 
	 * @return
	 */
	public Collection<ISuite> values() {
		return m_map.values();
	}
}
