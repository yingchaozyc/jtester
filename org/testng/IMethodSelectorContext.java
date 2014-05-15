package org.testng;

import java.util.Map;

/**
 * 方法选择器上下文对象接口。一般在IMethodSelector的includeMethod方法被调用的时候
 * 被当做参数传入。IMethodSelector可以在这个时候调用上下文对象里边的任何方法。
 * 
 * An implementation of this interface is passed to all the Method Selectors
 * when their includeMethod() is invoked. Method selectors can invoke any method
 * of this context at that time.
 * 
 * Created on Jan 3, 2007
 * 
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 */
public interface IMethodSelectorContext {

	/**
	 * 如果没有更多的方法选择器应该在目前任务之后被调用则返回true。 TODO
	 * 
	 * @return true if no more Method Selectors should be invoked after the
	 *         current one.
	 */
	public boolean isStopped();

	/**
	 * TODO 重新定义关闭状态
	 * 
	 * Indicate that no other Method Selectors should be invoked after the
	 * current one if stopped is false.
	 * 
	 * @param stopped
	 */
	public void setStopped(boolean stopped);

	/**
	 * 感觉上是一个很松散的方法。可以任意实现，你想存的数据都可以临时放到这里
	 * 
	 * @return a Map that can be freely manipulated by the Method Selector. This
	 *         can be used to share information among several Method Selectors.
	 */
	public Map<Object, Object> getUserData();
}
