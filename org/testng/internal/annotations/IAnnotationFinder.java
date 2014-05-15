package org.testng.internal.annotations;

import org.testng.annotations.IAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 这个接口定义了一种方法如何在类，方法，构造器上找到对应的注解。
 * 
 * 下边的这些注解的调用没有理解太清楚 TODO
 * 
 * This interface defines how annotations are found on classes, methods and
 * constructors. It will be implemented by both JDK 1.4 and JDK 5 annotation
 * finders.
 * 
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 */
@SuppressWarnings("rawtypes")
public interface IAnnotationFinder {

	/**
	 * 查看当前类有没有对应的注解。如果有返回注解对应的类，如果没有返回null。
	 * 
	 * @param cls
	 * @param annotationClass
	 * @return The annotation on the class or null if none found.
	 */ 
	public <A extends IAnnotation> A findAnnotation(Class cls,
			Class<A> annotationClass);

	/**
	 * 查看当前方法有没有对应的注解。如果有返回注解对应的类，如果没有返回null。
	 * 
	 * @param m
	 * @param annotationClass
	 * @return The annotation on the method. If not found, return the annotation
	 *         on the declaring class. If not found, return null.   注释 FIXME
	 */
	public <A extends IAnnotation> A findAnnotation(Method m,
			Class<A> annotationClass);

	/**
	 * 查看当前构造器有没有对应的注解。如果有返回注解对应的类，如果没有返回null。
	 * 
	 * @param cons
	 * @param annotationClass
	 * @return The annotation on the method. If not found, return the annotation
	 *         on the declaring class. If not found, return null.	注释 FIXME
	 */
	public <A extends IAnnotation> A findAnnotation(Constructor cons,
			Class<A> annotationClass);

	/**
	 * 没懂 TODO.
	 * 
	 * @return true if the ith parameter of the given method has the annotation
	 *         @TestInstance.
	 */
	public boolean hasTestInstance(Method method, int i);

	/**
	 * 大概理解，不是太懂 TODO
	 * 
	 * @return the @Optional values of this method's parameters (
	 *         <code>null</code> if the parameter isn't optional)
	 */
	public String[] findOptionalValues(Method method);

	/**
	 * 大概理解，不是太懂 TODO
	 * 
	 * @return the @Optional values of this method's parameters (
	 *         <code>null</code> if the parameter isn't optional)
	 */
	public String[] findOptionalValues(Constructor ctor);
}
