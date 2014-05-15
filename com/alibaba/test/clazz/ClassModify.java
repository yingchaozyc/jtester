package com.alibaba.test.clazz; 

import org.testng.internal.ClassHelper;

public class ClassModify {
	public static void main(String[] args) { 
		ClassHelper.tryOtherConstructor(ClassModifyEnum.class);
		System.out.println("done."); 
	}
}
