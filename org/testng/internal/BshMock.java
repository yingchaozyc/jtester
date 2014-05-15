package org.testng.internal;

import org.testng.ITestNGMethod;

/**
 * 粗暴的方式去伪mock。
 * 
 * @date 2014-5-15 下午4:55:10
 *
 */
public class BshMock implements IBsh {
    
	/**
	 * 果断的返回false表示不包括。
	 */
	@Override
	public boolean includeMethodFromExpression(String expression,
			ITestNGMethod tm) {
		return false;
	}

}
