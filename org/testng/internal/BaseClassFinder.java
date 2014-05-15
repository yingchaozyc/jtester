package org.testng.internal;

import org.testng.IClass;
import org.testng.ITestClassFinder;
import org.testng.ITestContext;
import org.testng.ITestObjectFactory;
import org.testng.collections.Maps;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;

import java.util.Map;

/**  
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 */
@SuppressWarnings("rawtypes")
abstract public class BaseClassFinder implements ITestClassFinder {
	
	// Class & IClass 之间的映射关系。
	private Map<Class, IClass> m_classes = Maps.newHashMap();
	
	/**
	 * 根据指定cls获取IClass。
	 */
	@Override
	public IClass getIClass(Class cls) {
		return m_classes.get(cls);
	}

	/**
	 * 插入映射关系
	 * 
	 * @param cls
	 * @param iClass
	 */
	protected void putIClass(Class cls, IClass iClass) {
		if (!m_classes.containsKey(cls)) {  	// 这个是否可以不判断? 反正都是覆盖 FIXME
			m_classes.put(cls, iClass);
		}
	}

	/**
	 * 先去映射关系查找IClass对象，如果找不到去出生成一个。
	 * 之后再放到映射关系中
	 * 
	 * @param cls
	 * @return An IClass for the given class, or null if we have already treated
	 *         this class.
	 */
	protected IClass findOrCreateIClass(ITestContext context, 
										 Class cls,
										 XmlClass xmlClass,
										 Object instance, 
										 XmlTest xmlTest,
										 IAnnotationFinder annotationFinder,
										 ITestObjectFactory objectFactory) {
		IClass result = m_classes.get(cls);
		if (null == result) {
			result = new ClassImpl(context, cls, xmlClass, instance, m_classes,
					xmlTest, annotationFinder, objectFactory);
			m_classes.put(cls, result);
		}

		return result;
	}

	protected Map getExistingClasses() {
		return m_classes;
	}

	/**
	 * 判断是否有cls这个测试类，只需判断map中有无这个key
	 * 
	 * @param cls
	 * @return
	 */
	protected boolean classExists(Class cls) {
		return m_classes.containsKey(cls);
	}

	/**
	 * 已经存在的映射关系的value列表以数组的方式输出即可。
	 */
	@Override
	public IClass[] findTestClasses() {
		return m_classes.values().toArray(new IClass[m_classes.size()]);
	}
}
