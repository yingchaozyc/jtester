package org.testng;

import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.xml.XmlSuite;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 测试套件。
 * 
 * Interface defining a Test Suite.
 * 
 * @author Cedric Beust, Aug 6, 2004
 * 
 */
public interface ISuite extends IAttributes {

	/**
	 * 套件名字
	 * 
	 * @return the name of this suite.
	 */
	public String getName();

	/**
	 * 套件返回的测试结果
	 * 
	 * @return The results for this suite.
	 */
	public Map<String, ISuiteResult> getResults();

	/**
	 * 对象工厂。被用来在各个测试用例中来创建对象。version1
	 * 
	 * @return The object factory used to create all test instances.
	 */
	public IObjectFactory getObjectFactory();

	/**
	 * 对象工厂。被用来在各个测试用例中来创建对象。version2
	 * 
	 * @return The object factory used to create all test instances.
	 */
	public IObjectFactory2 getObjectFactory2();

	/**
	 * 获取报表输出目录。
	 * 
	 * @return The output directory used for the reports.
	 */
	public String getOutputDirectory();

	/**
	 * 如果用例必须要并行运行返回true。
	 * 
	 * @return true if the tests must be run in parallel.
	 */
	public String getParallel();

	/**
	 * TODO 拿到parameterName对应的value值
	 * 
	 * @return The value of this parameter, or null if none was specified.
	 */
	public String getParameter(String parameterName);

	/**
	 * 返回这样一个Map，key是组名，value是这个组下的所有测试方法
	 * 
	 * Retrieves the map of groups and their associated test methods.
	 * 
	 * @return A map where the key is the group and the value is a list of
	 *         methods used by this group.
	 */
	public Map<String, Collection<ITestNGMethod>> getMethodsByGroups();

	/**
	 * 返回当前suite执行的所有测试方法的列表。不过已经过期不建议使用了。
	 * 目前使用getAllInvokedMthods().
	 * 
	 * Retrieves the list of all the methods that were invoked during this run.
	 * 
	 * @return a collection of ITestNGMethods belonging to all tests included in
	 *         the suite.
	 * @deprecated Use getAllInvokedMthods().
	 */
	@Deprecated
	public Collection<ITestNGMethod> getInvokedMethods();

	/**
	 * 返回当前suite执行的所有测试方法的列表。 .
	 * 
	 * @return a list of all the methods that were invoked in this suite.
	 */
	public List<IInvokedMethod> getAllInvokedMethods();

	/**
	 * 返回当前suite执行的所有包含的测试方法的列表。 
	 * 
	 * @return All the methods that were not included in this test run.
	 */
	public Collection<ITestNGMethod> getExcludedMethods();

	/**
	 * 启动!
	 * 
	 * Triggers the start of running tests included in the suite.
	 */
	public void run();

	/**
	 * 返回执行该suite的host信息。如果是本地执行返回null。
	 * 返回的字符串格式类似如下: 172.18.63.174:59678
	 * 
	 * @return The host where this suite was run, or null if it was run locally.
	 *         The returned string has the form: host:port
	 */
	public String getHost();

	/**
	 * 查看当前suite的运行状态
	 * 
	 * Retrieves the shared state for a suite.
	 * 
	 * @return the share state of the current suite.
	 */
	public SuiteRunState getSuiteState();

	/**
	 * 获取注解搜寻器
	 * 
	 * @return the annotation finder used for the specified type (JDK5 or javadoc)
	 */
	public IAnnotationFinder getAnnotationFinder();

	/**
	 * 返回代表了XML的suite对象。
	 * 
	 * @return The representation of the current XML suite file.
	 */
	public XmlSuite getXmlSuite();

	/**
	 * 添加监听器。
	 * 
	 * @param listener
	 */
	public void addListener(ITestNGListener listener);

	/**
	 * 得到全部方法(可能有一些隐含的在里边) TODO
	 * 
	 * @return the total number of methods found in this suite. The presence of
	 *         factories or data providers might cause the actual number of test
	 *         methods run be bigger than this list.
	 */
	List<ITestNGMethod> getAllMethods();
}
