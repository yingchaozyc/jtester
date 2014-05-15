package org.testng;

import java.lang.reflect.Constructor;

/**
 * 对象工厂。
 * 
 * Factory used to create all test instances. This factory is passed the
 * constructor along with the parameters that TestNG calculated based on the
 * environment (@Parameters, etc...).
 * 
 * @see IObjectFactory2
 * 
 * @author Hani Suleiman
 * @since 5.6
 */
@SuppressWarnings("rawtypes")
public interface IObjectFactory extends ITestObjectFactory {
	
	Object newInstance(Constructor constructor, Object... params);
	
}
