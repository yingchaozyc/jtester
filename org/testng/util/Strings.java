package org.testng.util;

import org.testng.collections.Lists;
import org.testng.collections.Maps;

import java.util.List;
import java.util.Map;

/**
 * TestNG所用到的字符串工具类
 *
 * @date 2014-5-15 下午4:19:54
 *
 */
public class Strings {
	/**
	 * TestNG判断是否是空串 如果string是"   "，则返回的是false。
	 * 
	 * @param string
	 * @return
	 */
	public static boolean isNullOrEmpty(String string) {
		return string == null || string.length() == 0; // string.isEmpty() in Java 6
	}
	
	/**
	 * & < >这三种符号在HTML页面转码。
	 * 
	 * @param text
	 * @return
	 */
	public static String escapeHtml(String text) {
		String result = text;
		for (Map.Entry<String, String> entry : ESCAPE_HTML_MAP.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}

	//--------------------------upstair is open using ...-----------------------------------
	
	// 下边这做法简直太扯淡了。。 为什么不能直接塞到MAP   FIXME
	private static List<String> ESCAPE_HTML_LIST = Lists.newArrayList("&",
			"&amp;", "<", "&lt;", ">", "&gt;");

	private static final Map<String, String> ESCAPE_HTML_MAP = Maps
			.newLinkedHashMap();

	static {
		for (int i = 0; i < ESCAPE_HTML_LIST.size(); i += 2) {
			ESCAPE_HTML_MAP.put(ESCAPE_HTML_LIST.get(i),
					ESCAPE_HTML_LIST.get(i + 1));
		}
	}


	public static void main(String[] args) {
		System.out.println(escapeHtml("10 < 20 && 30 > 20"));
	}
}
