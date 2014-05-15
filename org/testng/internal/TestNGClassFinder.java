package org.testng.internal;

import org.testng.IClass;
import org.testng.IInstanceInfo;
import org.testng.ITestContext;
import org.testng.ITestObjectFactory;
import org.testng.TestNGException;
import org.testng.annotations.IAnnotation;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.internal.annotations.AnnotationHelper;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class creates an ITestClass from a test class.
 * 
 * @author <a href="mailto:cedric@beust.com">Cedric Beust</a>
 */
@SuppressWarnings("rawtypes")
public class TestNGClassFinder extends BaseClassFinder {
	private ITestContext m_testContext = null;

	private Map<Class, List<Object>> m_instanceMap = Maps.newHashMap();

	/**
	 * TestNGClassFinder的构造器。做了很多事情
	 * 
	 * @param cim
	 * @param instanceMap
	 * @param xmlTest
	 * @param configuration
	 * @param testContext
	 *            从TestRunner过来的调用的话这个testContext就是TestRunner。
	 */
	public TestNGClassFinder(
			ClassInfoMap cim,
			Map<Class, List<Object>> instanceMap, 
			XmlTest xmlTest,
			IConfiguration configuration,
			ITestContext testContext) {
		m_testContext = testContext;

		if (null == instanceMap) {
			instanceMap = Maps.newHashMap();
		}

		// 从IConfiguration中获取对象工程和注解搜寻器。
		IAnnotationFinder annotationFinder = configuration.getAnnotationFinder();
		ITestObjectFactory objectFactory = configuration.getObjectFactory();

		//
		// 拿到ClassInfoMap中的key集合
		// Find all the new classes and their corresponding instances
		//
		Set<Class<?>> allClasses = cim.getClasses();
		
		// 整个流程第一次objectFactory准备创建
		// very first pass is to find ObjectFactory, can't create anything else
		// until then
		if (objectFactory == null) {
			objectFactory = new ObjectFactoryImpl();
			outer:
			for (Class cls : allClasses) {
				try {
					if (null != cls) {
						// 反射拿到所有的方法
						for (Method m : cls.getMethods()) {    
							// 查看方法上是否有IObjectFactoryAnnotation注解
							IAnnotation a = annotationFinder.findAnnotation(m, org.testng.annotations.IObjectFactoryAnnotation.class);
							if (null != a) {
								if (!ITestObjectFactory.class.isAssignableFrom(m.getReturnType())) {
									throw new TestNGException("Return type of " + m + " is not IObjectFactory");
								}
								
								try {
									Object instance = cls.newInstance();
									if (m.getParameterTypes().length > 0
											&& m.getParameterTypes()[0]
													.equals(ITestContext.class)) {
										objectFactory = (ITestObjectFactory) m
												.invoke(instance, testContext);
									} else {
										objectFactory = (ITestObjectFactory) m
												.invoke(instance);
									}
									break outer;
								} catch (Exception ex) {
									throw new TestNGException(
											"Error creating object factory: "
													+ cls, ex);
								}
							}
						}
					}
				} catch (NoClassDefFoundError e) {
					Utils.log("[TestNGClassFinder]", 1,
							"Unable to read methods on class " + cls.getName()
									+ " - unable to resolve class reference "
									+ e.getMessage());

					for (XmlClass xmlClass : xmlTest.getXmlClasses()) {
						if (xmlClass.loadClasses()
								&& xmlClass.getName().equals(cls.getName())) {
							throw e;
						}
					}

				}
			}
		}

		for (Class cls : allClasses) {   // com.alibaba.jtester.unit1.Test1
			if ((null == cls)) {
				ppp("FOUND NULL CLASS IN FOLLOWING ARRAY:");
				int i = 0;
				for (Class c : allClasses) {
					ppp("  " + i + ": " + c);
				}

				continue;
			}

			// 遍历寻找。
			// 从指定类自身开始一直到他的父类，如果有任何一个类中有TestNG的16种标记，
			// 则认为当前类就是TestNGClass。
			if (isTestNGClass(cls, annotationFinder)) {
				List allInstances = instanceMap.get(cls);   // 简单例子是空
				Object thisInstance = (null != allInstances) ? allInstances.get(0) : null; // 简单例子是空

				// If annotation class and instances are abstract, skip them
				// 给这个工具类点赞~~ java.lang.Modifier
				if ((null == thisInstance) && Modifier.isAbstract(cls.getModifiers())) {
					Utils.log("", 5,
							"[WARN] Found an abstract class with no valid instance attached: "
									+ cls);
					continue;
				}

				IClass ic = findOrCreateIClass(
						m_testContext,
						cls,
						cim.getXmlClass(cls), 
						thisInstance, 
						xmlTest,
						annotationFinder,
						objectFactory);
				if (null != ic) {
					Object[] theseInstances = ic.getInstances(false);
					if (theseInstances.length == 0) {
						theseInstances = ic.getInstances(true);
					}
					Object instance = theseInstances[0];
					putIClass(cls, ic);

					ConstructorOrMethod factoryMethod = ClassHelper.findDeclaredFactoryMethod(cls, annotationFinder);
					if (factoryMethod != null && factoryMethod.getEnabled()) {
						FactoryMethod fm = new FactoryMethod( /* cls, */
						factoryMethod, instance, xmlTest, annotationFinder,
								m_testContext);
						ClassInfoMap moreClasses = new ClassInfoMap();

						{ 
							Object[] instances = fm.invoke();

							//
							// If the factory returned IInstanceInfo, get the
							// class from it,
							// otherwise, just call getClass() on the returned
							// instances
							//
							if (instances.length > 0) {
								if (instances[0] != null) {
									Class elementClass = instances[0]
											.getClass();
									if (IInstanceInfo.class.isAssignableFrom(elementClass)) {
										for (Object o : instances) {
											IInstanceInfo ii = (IInstanceInfo) o;
											addInstance(ii.getInstanceClass(), ii.getInstance());
											moreClasses.addClass(ii.getInstanceClass());
										}
									} else {
										for (int i = 0; i < instances.length; i++) {
											Object o = instances[i];
											if (o == null) {
												throw new TestNGException(
														"The factory "
																+ fm
																+ " returned a null instance"
																+ "at index "
																+ i);
											} else {
												addInstance(o.getClass(), o);
												if (!classExists(o.getClass())) {
													moreClasses.addClass(o
															.getClass());
												}
											}
										}
									}
								}
							}
						}

						if (moreClasses.getSize() > 0) {
							TestNGClassFinder finder = new TestNGClassFinder(
									moreClasses, m_instanceMap, xmlTest,
									configuration, m_testContext);

							IClass[] moreIClasses = finder.findTestClasses();
							for (IClass ic2 : moreIClasses) {
								putIClass(ic2.getRealClass(), ic2);
							}
						} // if moreClasses.size() > 0
					}
				} // null != ic
			} // if not TestNG class
			else {
				Utils.log("TestNGClassFinder", 3, "SKIPPING CLASS " + cls
						+ " no TestNG annotations found");
			}
		} // for

		//
		// Add all the instances we found to their respective IClasses
		//
		for (Class c : m_instanceMap.keySet()) {
			List<Object> instances = m_instanceMap.get(c);
			for (Object instance : instances) {
				IClass ic = getIClass(c);
				if (null != ic) {
					ic.addInstance(instance);
				}
			}
		}
	}

	/**
	 * 遍历寻找。
	 * 从指定类自身开始一直到他的父类，如果有任何一个类中有TestNG的16种标记，
	 * 则认为当前类就是TestNGClass。
	 * 
	 * @returns true if this class contains TestNG annotations (either on itself
	 *          or on a superclass).
	 */
	@SuppressWarnings("unchecked")
	public static boolean isTestNGClass(Class c,
			IAnnotationFinder annotationFinder) {
		Class[] allAnnotations = AnnotationHelper.getAllAnnotations();
		Class cls = c;

		try {
			for (Class annotation : allAnnotations) {

				for (cls = c; cls != null; cls = cls.getSuperclass()) {
					// Try on the methods
					for (Method m : cls.getMethods()) {
						IAnnotation ma = annotationFinder.findAnnotation(m,
								annotation);
						if (null != ma) {
							return true;
						}
					}

					// Try on the class
					IAnnotation a = annotationFinder.findAnnotation(cls,
							annotation);
					if (null != a) {
						return true;
					}

					// Try on the constructors
					for (Constructor ctor : cls.getConstructors()) {
						IAnnotation ca = annotationFinder.findAnnotation(ctor,
								annotation);
						if (null != ca) {
							return true;
						}
					}
				}
			}

			return false;

		} catch (NoClassDefFoundError e) {
			Utils.log(
					"[TestNGClassFinder]",
					1,
					"Unable to read methods on class " + cls.getName()
							+ " - unable to resolve class reference "
							+ e.getMessage());
			return false;
		}
	}

	private void addInstance(Class clazz, Object o) {
		List<Object> list = m_instanceMap.get(clazz);

		if (null == list) {
			list = Lists.newArrayList();
			m_instanceMap.put(clazz, list);
		}

		list.add(o);
	}

	public static void ppp(String s) {
		System.out.println("[TestNGClassFinder] " + s);
	}

}
