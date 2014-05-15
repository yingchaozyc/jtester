package org.testng.internal;

import org.testng.IObjectFactory;
import org.testng.TestNGException;

import java.lang.reflect.Constructor;

/**
 * 默认的对象工厂实现类。
 * 
 * Default factory for test creation. Note that if no constructor is found
 * matching the specified parameters, this factory will try to invoke a
 * constructor that takes in a string object
 * 
 * @author Hani Suleiman Date: Mar 6, 2007 Time: 12:00:27 PM
 * @since 5.6
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ObjectFactoryImpl implements IObjectFactory {

	/**
   *
   */
	private static final long serialVersionUID = -4547389328475540017L;
 
	/**
	 * 用构造器和参数来实例化。
	 * 这里的构造器和参数一定是需要对应的，变量参数吻合。
	 */
	@Override
	public Object newInstance(Constructor constructor, Object... params) {
		try {
			// 霸道! 强制开启访问!
			constructor.setAccessible(true);
			return constructor.newInstance(params);
		} catch (IllegalAccessException ex) {
			// 如果权限不足，用单字符串参数去构造实例化
			return ClassHelper.tryOtherConstructor(constructor.getDeclaringClass());
		} catch (InstantiationException ex) {
			// 如果不能实例化，用单字符串参数去构造实例化
			return ClassHelper.tryOtherConstructor(constructor.getDeclaringClass());
		} catch (Exception ex) {
			throw new TestNGException("Cannot instantiate class "
					+ (constructor != null ? constructor.getDeclaringClass()
							.getName()
							: ": couldn't find a suitable constructor"), ex);
		}
	}
}
