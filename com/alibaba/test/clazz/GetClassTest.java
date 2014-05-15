package com.alibaba.test.clazz;

public class GetClassTest {
	@SuppressWarnings("rawtypes")
	public static void main(String[] args) { 
		for (Class c : GetClassTest.class.getClasses()) {
			System.out.println(c.getClass().getName());
		}
	}
	
	private class A{
		
	}

	class B extends A{
		
	}

	class C extends B{
		
	}

	interface D{
		
	}

	class E extends C implements D {
		
	}
} 
