package org.testng.internal.annotations;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * IAnnotationTransformer的默认实现。 X， 没有实现。 
 *
 * @date 2014-5-13 下午5:32:25
 *
 */
public class DefaultAnnotationTransformer implements IAnnotationTransformer {

	@Override
	public void transform(ITestAnnotation annotation, Class testClass,
			Constructor testConstructor, Method testMethod) {
	}

}
