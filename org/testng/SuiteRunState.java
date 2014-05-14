package org.testng;

import java.io.Serializable;

/**
 * 一个记录了suite的运行状态的对象。
 * A state object that records the status of the suite run. 
 * 
 * 这一句不知道怎么理解 TODO
 * Mainly used to figure out if there are any @BeforeSuite failures.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class SuiteRunState implements Serializable { 
	
	private static final long serialVersionUID = -2716934905049123874L;
	
	// 是否有失败的状态记录值
	private boolean m_hasFailures;

	/**
	 * 有失败，标记为true。
	 */
	public synchronized void failed() {
		m_hasFailures = true;
	}

	/**
	 * 查看状态是否失败。
	 * 
	 * @return
	 */
	public synchronized boolean isFailed() {
		return m_hasFailures;
	}
}
