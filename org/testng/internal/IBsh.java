package org.testng.internal;

import org.testng.ITestNGMethod;

/**
 * beanshell解析判断接口。
 * 
 * @date 2014-5-15 下午4:54:00
 *
 */
public interface IBsh {
	/**
	 * 表达式是否有正确的方法
	 * 
	 * @param expression
	 * @param tm
	 * @return
	 */
	boolean includeMethodFromExpression(String expression, ITestNGMethod tm);
}
