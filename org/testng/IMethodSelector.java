package org.testng;

import java.io.Serializable;
import java.util.List;

/**
 * 这个接口是被用来增强或者替换TestNG的算法，可以用来决定一个方法
 * 是否应该包括在一个测试用例里边。
 * 
 * This interface is used to augment or replace TestNG's algorithm to decide
 * whether a test method should be included in a test run.
 * 
 * Created on Sep 26, 2005
 * 
 * @author cbeust
 */
public interface IMethodSelector extends Serializable {

	/**
	 * 决定当前方法method是否应该被包括在测试用例中。
	 * 
	 * 生杀大权的方法。。。
	 * 
	 * @param context
	 *            The selector context. The implementation of this method can
	 *            invoke setHalted(true) to indicate that no other Method
	 *            Selector should be invoked by TestNG after this one.
	 *            Additionally, this implementation can manipulate the Map
	 *            object returned by getUserData().
	 * @param method
	 *            The test method
	 * @param isTestMethod
	 *            true if this is a @Test method, false if it's a configuration
	 *            method
	 * @return true if this method should be included in the test run, false
	 *         otherwise
	 */
	public boolean includeMethod(IMethodSelectorContext context,
			ITestNGMethod method, boolean isTestMethod);

	/**
	 * 当所有的测试方法已经已知的时候可以调用来进行添加替换。
	 * 
	 * Invoked when all the test methods are known so that the method selector
	 * can perform additional work, such as adding the transitive closure of all
	 * the groups being included and depended upon.
	 */
	public void setTestMethods(List<ITestNGMethod> testMethods);

}
