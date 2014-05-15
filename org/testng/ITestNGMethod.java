package org.testng;

import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlTest;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Describes a TestNG annotated method and the instance on which it will be
 * invoked.
 * 
 * This interface is not meant to be implemented by users.
 * 
 * @author Cedric Beust, May 3, 2004
 */
@SuppressWarnings("rawtypes")
public interface ITestNGMethod extends Comparable, Serializable, Cloneable {

	/**
	 * 获取到方法对应的实际Class。 和Class.getDeclaringClass()还是有不同的。
	 * 
	 * class B extends A{}
	 * 
	 * 假设A里边有单元测试  那么根据B而来的数据如下：
	 * RealClass：		A
	 * DeclaringClass： B
	 * 
	 * @return The real class on which this method was declared (can be
	 *          different from getMethod().getDeclaringClass() if the test method
	 *          was defined in a superclass). 
	 */
	Class getRealClass();

	/**
	 * 获取包装后的ITestClass类
	 * 
	 * @return
	 */
	ITestClass getTestClass();

	/**
	 * 设置包装类ITestClass。 这个也要公开?有点危险啊。FIXME
	 * 
	 * Sets the test class having this method. This is not necessarily the
	 * declaring class.
	 * 
	 * @param cls  The test class having this method.
	 */
	void setTestClass(ITestClass cls);

	/**
	 * 得到当前执行的这个方法对应的java反射类。
	 * 
	 * PS: 已经过期。后续建议调用getConstructorOrMethod()。
	 * 
	 * @return the corresponding Java test method.
	 * @deprecated This method is deprecated and can now return null. Use
	 *             getConstructorOrMethod() instead.
	 */
	@Deprecated
	Method getMethod();

	/**
	 * 返回方法名称。(这里需要序列化?什么意思??TODO)
	 * 
	 * Returns the method name. This is needed for serialization because methods
	 * are not Serializable.
	 * 
	 * @return the method name.
	 */
	String getMethodName();

	/**
	 * 返回调用者的数组 TODO
	 * 
	 * @return All the instances the methods will be invoked upon. This will
	 *         typically be an array of one object in the absence of an @Factory
	 *         annotation.
	 * 
	 * @deprecated Use getInstance().
	 */
	@Deprecated
	Object[] getInstances();

	/**
	 * 返回调用者。这里怎么又成一个了? TODO
	 * 
	 * @return
	 */
	Object getInstance();

	/**
	 * 不明 TODO
	 * 
	 * Needed for serialization.
	 */
	long[] getInstanceHashCodes();

	/**
	 * @return The groups this method belongs to, possibly added to the groups
	 *         declared on the class.
	 */
	String[] getGroups();

	/**
	 * @return The groups this method depends on, possibly added to the groups
	 *         declared on the class.
	 */
	String[] getGroupsDependedUpon();

	/**
	 * If a group was not found.
	 */
	String getMissingGroup();

	public void setMissingGroup(String group);

	/**
	 * Before and After groups
	 */
	public String[] getBeforeGroups();

	public String[] getAfterGroups();

	/**
	 * @return The methods this method depends on, possibly added to the methods
	 *         declared on the class.
	 */
	String[] getMethodsDependedUpon();

	void addMethodDependedUpon(String methodName);

	/**
	 * @return true if this method was annotated with @Test
	 */
	boolean isTest();

	/**
	 * @return The timeout in milliseconds.
	 */
	long getTimeOut();

	void setTimeOut(long timeOut);

	/**
	 * @return the number of times this method needs to be invoked.
	 */
	int getInvocationCount();

	void setInvocationCount(int count);

	/**
	 * @return the success percentage for this method (between 0 and 100).
	 */
	int getSuccessPercentage();

	/**
	 * @return The id of the thread this method was run in.
	 */
	String getId();

	void setId(String id);

	long getDate();

	void setDate(long date);

	/**
	 * Returns if this ITestNGMethod can be invoked from within IClass.
	 */
	boolean canRunFromClass(IClass testClass);

	/**
	 * @return true if this method is alwaysRun=true
	 */
	boolean isAlwaysRun();

	/**
	 * @return the number of threads to be used when invoking the method on
	 *         parallel
	 */
	int getThreadPoolSize();

	void setThreadPoolSize(int threadPoolSize);

	boolean getEnabled();

	public String getDescription();

	public void incrementCurrentInvocationCount();

	public int getCurrentInvocationCount();

	public void setParameterInvocationCount(int n);

	public int getParameterInvocationCount();

	public ITestNGMethod clone();

	public IRetryAnalyzer getRetryAnalyzer();

	public void setRetryAnalyzer(IRetryAnalyzer retryAnalyzer);

	public boolean skipFailedInvocations();

	public void setSkipFailedInvocations(boolean skip);

	/**
	 * The time under which all invocationCount methods need to complete by.
	 */
	public long getInvocationTimeOut();

	public boolean ignoreMissingDependencies();

	public void setIgnoreMissingDependencies(boolean ignore);

	/**
	 * Which invocation numbers of this method should be used (only applicable
	 * if it uses a data provider). If this value is an empty list, use all the
	 * values returned from the data provider. These values are read from the
	 * XML file in the <include invocationNumbers="..."> tag.
	 */
	public List<Integer> getInvocationNumbers();

	public void setInvocationNumbers(List<Integer> numbers);

	/**
	 * The list of invocation numbers that failed, which is only applicable for
	 * methods that have a data provider.
	 */
	public void addFailedInvocationNumber(int number);

	public List<Integer> getFailedInvocationNumbers();

	/**
	 * The scheduling priority. Lower priorities get scheduled first.
	 */
	public int getPriority();

	public void setPriority(int priority);

	/**
	 * @return the XmlTest this method belongs to.
	 */
	public XmlTest getXmlTest();

	ConstructorOrMethod getConstructorOrMethod();

	/**
	 * @return the parameters found in the include tag, if any
	 * @param test
	 */
	Map<String, String> findMethodParameters(XmlTest test);
	

	//--------------------------------------------------一些方便的注解判断，开始------------------------------------------------------------
	/**
	 * @return true if this method was annotated with @Configuration and
	 *         beforeTestMethod = true
	 */
	boolean isBeforeMethodConfiguration();

	/**
	 * @return true if this method was annotated with @Configuration and
	 *         beforeTestMethod = false
	 */
	boolean isAfterMethodConfiguration();

	/**
	 * @return true if this method was annotated with @Configuration and
	 *         beforeClassMethod = true
	 */
	boolean isBeforeClassConfiguration();

	/**
	 * @return true if this method was annotated with @Configuration and
	 *         beforeClassMethod = false
	 */
	boolean isAfterClassConfiguration();

	/**
	 * @return true if this method was annotated with @Configuration and
	 *         beforeSuite = true
	 */
	boolean isBeforeSuiteConfiguration();

	/**
	 * @return true if this method was annotated with @Configuration and
	 *         afterSuite = true
	 */
	boolean isAfterSuiteConfiguration();

	/**
	 * @return <tt>true</tt> if this method is a @BeforeTest (@Configuration
	 *         beforeTest=true)
	 */
	boolean isBeforeTestConfiguration();

	/**
	 * @return <tt>true</tt> if this method is an @AfterTest (@Configuration
	 *         afterTest=true)
	 */
	boolean isAfterTestConfiguration();

	boolean isBeforeGroupsConfiguration();

	boolean isAfterGroupsConfiguration(); 
}
