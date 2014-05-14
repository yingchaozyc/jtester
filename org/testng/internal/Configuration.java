package org.testng.internal; 

import org.testng.IConfigurable;
import org.testng.IConfigurationListener;
import org.testng.IExecutionListener;
import org.testng.IHookable;
import org.testng.ITestObjectFactory;
import org.testng.collections.Lists;
import org.testng.internal.annotations.DefaultAnnotationTransformer;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.internal.annotations.JDK15AnnotationFinder;

import java.util.List;

/**
 * 核心配置接口IConfiguration的默认实现
 * 
 * @date 2014-5-13 下午5:18:46
 * 
 */
public class Configuration implements IConfiguration {

	// 注解寻找器。 在TestNG初始化时候同时初始化当前类构造器赋值。
	IAnnotationFinder m_annotationFinder;
	
	ITestObjectFactory m_objectFactory;
	IHookable m_hookable;
	IConfigurable m_configurable;
	List<IExecutionListener> m_executionListeners = Lists.newArrayList();

	private List<IConfigurationListener> m_configurationListeners = Lists
			.newArrayList();

	/**
	 * DefaultAnnotationTransformer是一个注解转换器。感觉上可以随心所欲替换你的注解。
	 */
	public Configuration() {
		// new DefaultAnnotationTransformer()  初始化注解转换器(但是实现类什么都没做)
		// new JDK15AnnotationFinder(IAnnotationTransformer transformer) 初始化注解map
		// init(IAnnotationFinder finder) 初始化IAnnotationFinder值。
		init(new JDK15AnnotationFinder(new DefaultAnnotationTransformer()));
	}

	public Configuration(IAnnotationFinder finder) {
		init(finder);
	}

	/**
	 * 初始化IAnnotationFinder值。
	 * 
	 * @param finder
	 */
	private void init(IAnnotationFinder finder) {
		m_annotationFinder = finder;
	}

	@Override
	public IAnnotationFinder getAnnotationFinder() {
		return m_annotationFinder;
	}

	@Override
	public void setAnnotationFinder(IAnnotationFinder finder) {
		m_annotationFinder = finder;
	}

	@Override
	public ITestObjectFactory getObjectFactory() {
		return m_objectFactory;
	}

	@Override
	public void setObjectFactory(ITestObjectFactory factory) {
		m_objectFactory = factory;
	}

	@Override
	public IHookable getHookable() {
		return m_hookable;
	}

	@Override
	public void setHookable(IHookable h) {
		m_hookable = h;
	}

	@Override
	public IConfigurable getConfigurable() {
		return m_configurable;
	}

	@Override
	public void setConfigurable(IConfigurable c) {
		m_configurable = c;
	}

	@Override
	public List<IExecutionListener> getExecutionListeners() {
		return m_executionListeners;
	}

	@Override
	public void addExecutionListener(IExecutionListener l) {
		m_executionListeners.add(l);
	}

	@Override
	public List<IConfigurationListener> getConfigurationListeners() {
		return Lists.newArrayList(m_configurationListeners);
	}

	@Override
	public void addConfigurationListener(IConfigurationListener cl) {
		if (!m_configurationListeners.contains(cl)) {
			m_configurationListeners.add(cl);
		}
	}
}