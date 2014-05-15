package org.testng;

/**
 * 用来定位测试类。
 * 
 * This class is used by TestNG to locate the test classes.
 * 
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 */
@SuppressWarnings("rawtypes")
public interface ITestClassFinder {
	/**
	 * 返回所有包含测试类的IClass数组。 底下的官方注释看的不是太懂 TODO
	 * 
	 * @return An array of all the classes that contain test methods. This
	 *         method usually returns an array of one class, which is the class
	 *         on which TestNG is running, except in the following cases. -
	 *         TestNG: the class contains an @Factory method - JUnit: the class
	 *         contains a suite() method
	 */
	public IClass[] findTestClasses();

	/**
	 * 返回cls对应的IClass类，这应该是一种转换。
	 * 
	 * Return the IClass for a given class
	 */
	public IClass getIClass(Class cls);

}
