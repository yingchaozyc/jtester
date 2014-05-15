package org.testng;

import org.testng.xml.XmlTest;

/**
 * 意思大致明白 不好写出 TODO
 * 
 * This interface allows to modify the strategy used by TestRunner to find its
 * test methods. At the time of this writing, TestNG supports two different
 * strategies: TestNG (using annotations to locate these methods) and JUnit
 * (setUp()/tearDown() and all methods that start with "test" or have a suite()
 * method).
 * 
 * @author Cedric Beust, May 3, 2004
 * 
 */
@SuppressWarnings("rawtypes")
public interface ITestMethodFinder {

	/**
	 * 返回Test节点下某个Class的所有测试方法，返回的会是一个数组。
	 * 
	 * @return All the applicable test methods.
	 */ 
	ITestNGMethod[] getTestMethods(Class cls, XmlTest xmlTest);

	/**
	 * TODO 为什么上边要限制在XmlTest节点，下边的就都不需要了?
	 * 
	 * 获得class中的前置方法列表。
	 * 
	 * @return All the methods that should be invoked before a test method is
	 *         invoked.
	 */
	ITestNGMethod[] getBeforeTestMethods(Class cls);

	/**
	 * 获得class中的后置方法列表。
	 * 
	 * @return All the methods that should be invoked after a test method
	 *         completes.
	 */
	ITestNGMethod[] getAfterTestMethods(Class cls);

	/**
	 * 获得class中的前置类方法列表。
	 * 
	 * @return All the methods that should be invoked after the test class has
	 *         been created and before any of its test methods is invoked.
	 */
	ITestNGMethod[] getBeforeClassMethods(Class cls);

	/**
	 * 获得class中的后置类方法列表。
	 * 
	 * @return All the methods that should be invoked after the test class has
	 *         been created and after all its test methods have completed.
	 */
	ITestNGMethod[] getAfterClassMethods(Class cls);

	/**
	 * 获得class中的前置suite方法列表。
	 * 
	 * @return All the methods that should be invoked before the suite starts
	 *         running.
	 */
	ITestNGMethod[] getBeforeSuiteMethods(Class cls);

	/**
	 * 获得class中的后置suite方法列表。
	 * 
	 * @return All the methods that should be invoked after the suite has run
	 *         all its tests.
	 */
	ITestNGMethod[] getAfterSuiteMethods(Class cls);

	/**
	 * 下边四个方法不明白 TODO
	 * 
	 * @param testClass
	 * @return
	 */
	ITestNGMethod[] getBeforeTestConfigurationMethods(Class testClass);

	ITestNGMethod[] getAfterTestConfigurationMethods(Class testClass);

	ITestNGMethod[] getBeforeGroupsConfigurationMethods(Class testClass);

	ITestNGMethod[] getAfterGroupsConfigurationMethods(Class testClass);

}
