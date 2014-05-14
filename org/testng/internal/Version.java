package org.testng.internal;

/**
 * TestNG版本信息。
 * 
 * @date 2014-5-14 下午5:52:06
 *
 */
public class Version {
	public static final String VERSION = "6.8.2beta_20130330_0839";

	public static void displayBanner() {
		System.out.println("...\n... TestNG " + VERSION
				+ " by Cédric Beust (cedric@beust.com)\n...\n");
	}
}
