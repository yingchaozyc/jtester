package org.testng.internal;

/**
 * 确认是否加载了beanshell.jar。
 * 这尼玛写的也够粗暴啊。
 * 
 * Determine the availability of certain jar files at runtime.
 */
public class Dynamic {

	public static boolean hasBsh() {
		try {
			Class.forName("bsh.Interpreter");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
