package org.testng.collections;

import java.util.Collection;
import java.util.Map;

/**
 * 集合处理工具类。
 * 
 * @date 2014-5-16 下午3:32:20
 *
 */
public class CollectionUtils {

	/**
	 * 判断Collection中是否有元素。
	 * 
	 * @param c
	 * @return
	 */
	public static boolean hasElements(Collection<?> c) {
		return c != null && !c.isEmpty();
	}

	/**
	 * 判断Map中是否有元素。
	 * 
	 * @param c
	 * @return
	 */
	public static boolean hasElements(Map<?, ?> c) {
		return c != null && !c.isEmpty();
	}

}
