package org.testng.internal;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.testng.IMethodSelector;
import org.testng.IMethodSelectorContext;
import org.testng.ITestNGMethod;
import org.testng.TestNGException;
import org.testng.collections.ListMultiMap;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;

/**
 * 当前是默认的method selecotr的实现。根据testng.xml的内容来决定一个方法
 * 哪些该include哪些改exlude。
 * 
 * This class is the default method selector used by TestNG to determine which
 * methods need to be included and excluded based on the specification given in
 * testng.xml.
 * 
 * Created on Sep 30, 2005
 * 
 * @author cbeust
 */
@SuppressWarnings({"unused","deprecation","rawtypes"})
public class XmlMethodSelector implements IMethodSelector {
	private static final long serialVersionUID = -9030548178025605629L;

	// Groups included and excluded for this run 
	private Map<String, String> m_includedGroups = Maps.newHashMap();
	private Map<String, String> m_excludedGroups = Maps.newHashMap();
	
	private List<XmlClass> m_classes = null;
	
	// List of methods included implicitly
	private ListMultiMap<String, XmlInclude> m_includedMethods = Maps.newListMultiMap();
	
	// 当前测试方法对应的beanshell表达式，如果没有该值是null
	// The BeanShell expression for this test, if any
	private String m_expression = null;
	// 使用哪种beanshell解析。
	private IBsh m_bsh = Dynamic.hasBsh() ? new Bsh() : new BshMock();
	 
	// 是否初始化，默认刚进来当然是false
	private boolean m_isInitialized = false;
	
	private List<ITestNGMethod> m_testMethods = null;

	@Override
	public boolean includeMethod(IMethodSelectorContext context,
			ITestNGMethod tm, boolean isTestMethod) { 
		if (!m_isInitialized) {
			m_isInitialized = true;
			
			// 实际的循环在init方法中，对于每一个方法的判断在下边
			init(context);
		}

		boolean result = false;
		
		// 如果有beanshell表达式，则用beanshell解析器去解析看是否应该包括该方法
		// 否则走普通判断
		if (null != m_expression) {
			return m_bsh.includeMethodFromExpression(m_expression, tm);
		} else {
			result = includeMethodFromIncludeExclude(tm, isTestMethod);
		}

		return result;
	}
  
	private boolean includeMethodFromIncludeExclude(ITestNGMethod tm,
			boolean isTestMethod) {
		boolean result = false;
		Method m = tm.getMethod();
		String[] groups = tm.getGroups();
		Map<String, String> includedGroups = m_includedGroups;
		Map<String, String> excludedGroups = m_excludedGroups;
		List<XmlInclude> includeList = m_includedMethods.get(MethodHelper
				.calculateMethodCanonicalName(tm));
 
		// No groups were specified: 
		if (includedGroups.size() == 0 
			&& excludedGroups.size() == 0
			&& !hasIncludedMethods()
			&& !hasExcludedMethods()) {
	 
			// If we don't include or exclude any methods, method is in 
			result = true;
		} else if (includedGroups.size() == 0 && excludedGroups.size() == 0
				&& !isTestMethod) {
			
			// If it's a configuration method and no groups were requested, we want it in
			result = true;
		} else if (includeList != null) {
			// 简单的测试方法会直接走到这一步
			// Is this method included implicitly?
			result = true;
		} else {
			// Include or Exclude groups were specified: 
			//
			// Only add this method if it belongs to an included group and not
			// to an excluded group
			//
			{
				boolean isIncludedInGroups = isIncluded(groups, m_includedGroups.values());
				boolean isExcludedInGroups = isExcluded(groups, m_excludedGroups.values());
 
				// Calculate the run methods by groups first 
				if (isIncludedInGroups && !isExcludedInGroups) {
					result = true;
				} else if (isExcludedInGroups) {
					result = false;
				}
			}

			if (isTestMethod) {
				//
				// Now filter by method name
				//
				Method method = tm.getMethod();
				Class methodClass = method.getDeclaringClass();
				String fullMethodName = methodClass.getName() + "."
						+ method.getName();

				String[] fullyQualifiedMethodName = new String[] { fullMethodName };

				//
				// Iterate through all the classes so we can gather all the
				// included and
				// excluded methods
				//
				for (XmlClass xmlClass : m_classes) {
					// Only consider included/excluded methods that belong to
					// the same class
					// we are looking at
					Class cls = xmlClass.getSupportClass();
					if (!assignable(methodClass, cls)) {
						continue;
					}

					List<String> includedMethods = createQualifiedMethodNames(
							xmlClass, toStringList(xmlClass.getIncludedMethods()));
					boolean isIncludedInMethods = isIncluded(
							fullyQualifiedMethodName, includedMethods);
					List<String> excludedMethods = createQualifiedMethodNames(
							xmlClass, xmlClass.getExcludedMethods());
					boolean isExcludedInMethods = isExcluded(
							fullyQualifiedMethodName, excludedMethods);
					if (result) {
						// If we're about to include this method by group, make
						// sure
						// it's included by method and not excluded by method
						result = isIncludedInMethods && !isExcludedInMethods;
					}
					// otherwise it's already excluded and nothing will bring it
					// back,
					// since exclusions preempt inclusions
				}
			}
		}

		Package pkg = m.getDeclaringClass().getPackage();
		String methodName = pkg != null ? pkg.getName() + "." + m.getName() : m
				.getName();

		logInclusion(result ? "Including" : "Excluding", "method", methodName
				+ "()");

		return result;
	}

	@SuppressWarnings({ "unchecked" })
	private boolean assignable(Class sourceClass, Class targetClass) {
		return sourceClass.isAssignableFrom(targetClass)
				|| targetClass.isAssignableFrom(sourceClass);
	}

	private Map<String, String> m_logged = Maps.newHashMap();

	private void logInclusion(String including, String type, String name) {
		if (!m_logged.containsKey(name)) {
			log(4, including + " " + type + " " + name);
			m_logged.put(name, name);
		}
	}

	private boolean hasIncludedMethods() {
		for (XmlClass xmlClass : m_classes) {
			if (xmlClass.getIncludedMethods().size() > 0) {
				return true;
			}
		}

		return false;
	}

	private boolean hasExcludedMethods() {
		for (XmlClass xmlClass : m_classes) {
			if (xmlClass.getExcludedMethods().size() > 0) {
				return true;
			}
		}

		return false;
	}

	private List<String> toStringList(List<XmlInclude> methods) {
		List<String> result = Lists.newArrayList();
		for (XmlInclude m : methods) {
			result.add(m.getName());
		}
		return result;
	}

	private List<String> createQualifiedMethodNames(XmlClass xmlClass,
			List<String> methods) {
		List<String> vResult = Lists.newArrayList();
		Class cls = xmlClass.getSupportClass();

		while (null != cls) {
			for (String im : methods) {
				String methodName = im;
				Method[] allMethods = cls.getDeclaredMethods();
				Pattern pattern = Pattern.compile(methodName);
				for (Method m : allMethods) {
					if (pattern.matcher(m.getName()).matches()) {
						vResult.add(makeMethodName(cls.getName(), m.getName()));
					}
				}
			}
			cls = cls.getSuperclass();
		}

		return vResult;
	}
	
	/**
	 * 构建一个类名+方法名的组合名称
	 * 
	 * @param className
	 * @param methodName
	 * @return
	 */
	private String makeMethodName(String className, String methodName) {
		return className + "." + methodName;
	}

	private void checkMethod(String className, String methodName) {
		Pattern p = Pattern.compile(methodName);
		try {
			Class<?> c = Class.forName(className);
			for (Method m : c.getMethods()) {
				if (p.matcher(m.getName()).matches()) {
					return;
				}
			}
		} catch (ClassNotFoundException e) {
			throw new TestNGException(e);
		}

		Utils.log("Warning", 2, "The regular exception \"" + methodName
				+ "\" didn't match any" + " method in class " + className);
	}

	public void setXmlClasses(List<XmlClass> classes) {
		m_classes = classes;
		for (XmlClass c : classes) {
			for (XmlInclude m : c.getIncludedMethods()) {
				checkMethod(c.getName(), m.getName());
				String methodName = makeMethodName(c.getName(), m.getName());
				m_includedMethods.put(methodName, m);
			}
		}
	}

	/**
	 * @return Returns the excludedGroups.
	 */
	public Map<String, String> getExcludedGroups() {
		return m_excludedGroups;
	}

	/**
	 * @return Returns the includedGroups.
	 */
	public Map<String, String> getIncludedGroups() {
		return m_includedGroups;
	}

	/**
	 * @param excludedGroups
	 *            The excludedGroups to set.
	 */
	public void setExcludedGroups(Map<String, String> excludedGroups) {
		m_excludedGroups = excludedGroups;
	}

	/**
	 * @param includedGroups
	 *            The includedGroups to set.
	 */
	public void setIncludedGroups(Map<String, String> includedGroups) {
		m_includedGroups = includedGroups;
	}

	private static boolean isIncluded(String[] groups,
			Collection<String> includedGroups) {
		if (includedGroups.size() == 0) {
			return true;
		} else {
			return isMemberOf(groups, includedGroups);
		}
	}

	private static boolean isExcluded(String[] groups,
			Collection<String> excludedGroups) {
		return isMemberOf(groups, excludedGroups);
	}

	/**
	 * 
	 * @param groups
	 *            Array of groups on the method
	 * @param list
	 *            Map of regexps of groups to be run
	 */
	private static boolean isMemberOf(String[] groups, Collection<String> list) {
		for (String group : groups) {
			for (Object o : list) {
				String regexpStr = o.toString();
				boolean match = Pattern.matches(regexpStr, group);
				if (match) {
					return true;
				}
			}
		}

		return false;
	}

	private static void log(int level, String s) {
		Utils.log("XmlMethodSelector", level, s);
	} 

	public void setExpression(String expression) {
		m_expression = expression;
	} 

	@Override
	public void setTestMethods(List<ITestNGMethod> testMethods) {
		// Caution: this variable is initialized with an empty list first and
		// then modified
		// externally by the caller (TestRunner#fixMethodWithClass). Ugly.
		m_testMethods = testMethods;
	}

	private void init(IMethodSelectorContext context) {
		String[] groups = m_includedGroups.keySet().toArray(new String[m_includedGroups.size()]); // 开始是空
		Set<String> groupClosure = new HashSet<String>();
		Set<ITestNGMethod> methodClosure = new HashSet<ITestNGMethod>();

		List<ITestNGMethod> includedMethods = Lists.newArrayList();
		for (ITestNGMethod m : m_testMethods) {
			if (includeMethod(context, m, true)) {
				includedMethods.add(m);
			}
		}
		
		// 在最简单的例子里边, group这个概念都是没有的。 
		// includedMethods是List<ITestNGMethod>, 已经包括进来的方法
		// m_testMethod是List<ITestNGMethod>, 是全部包括进来的方法
		// 在最简单的例子中，只是runningMethods这个集合塞了一个值而已。 TODO
		// 这个方法到底要干什么其实是没太懂的。
		MethodGroupsHelper.findGroupTransitiveClosure(
				this, 
				includedMethods,
				m_testMethods,
				groups,
				groupClosure,
				methodClosure);

		// 又是和Group有关。 暂时跳过了。 TODO
		//
		// If we are asked to include or exclude specific groups, calculate
		// the transitive closure of all the included groups. If no include
		// groups
		// were specified, don't do anything.
		// Any group that is part of the transitive closure but not part of
		// m_includedGroups is being added implicitly by TestNG so that if
		// someone
		// includes a group z that depends on a, b and c, they don't need to
		// include a, b and c explicitly.
		if (m_includedGroups.size() > 0) {
			// Make the transitive closure our new included groups
			for (String g : groupClosure) {
				log(4, "Including group "
						+ (m_includedGroups.containsKey(g) ? ": "
								: "(implicitly): ") + g);
				m_includedGroups.put(g, g);
			}

			// Make the transitive closure our new included methods
			for (ITestNGMethod m : methodClosure) {
				String methodName = m.getMethod().getDeclaringClass().getName()
						+ "." + m.getMethodName(); 

				List<XmlInclude> includeList = m_includedMethods
						.get(methodName);
				XmlInclude xi = new XmlInclude(methodName);
				// TODO: set the XmlClass on this xi or we won't get inheritance of parameters
				m_includedMethods.put(methodName, xi);
				logInclusion("Including", "method ", methodName);
			}
		}
	}
}
