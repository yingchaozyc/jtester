package org.testng;

/**
 * 注释太拗口了 TODO
 * 
 * This interface captures a test method along with all the instances it should
 * be run on.
 */
public interface IMethodInstance {

	/**
	 * 获取ITestNGMethod方法
	 * 
	 * @return
	 */
	ITestNGMethod getMethod();

	/**
	 * 过期的方法。获取到对应的实例数组。
	 * 
	 * @deprecated Use getInstance()
	 */
	Object[] getInstances();

	/**
	 * 获取对应的唯一实例。
	 * 
	 * @return
	 */
	Object getInstance();
}
