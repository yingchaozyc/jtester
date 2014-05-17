package org.testng;

import static org.testng.internal.Utils.defaultIfStringEmpty;
import static org.testng.internal.Utils.isStringEmpty;
import static org.testng.internal.Utils.isStringNotEmpty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.ITestAnnotation;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.internal.ClassHelper;
import org.testng.internal.Configuration;
import org.testng.internal.DynamicGraph;
import org.testng.internal.IConfiguration;
import org.testng.internal.IResultListener2;
import org.testng.internal.OverrideProcessor;
import org.testng.internal.SuiteRunnerMap;
import org.testng.internal.Utils;
import org.testng.internal.Version;
import org.testng.internal.annotations.DefaultAnnotationTransformer;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.internal.annotations.JDK15AnnotationFinder;
import org.testng.internal.annotations.Sets;
import org.testng.internal.thread.graph.GraphThreadPoolExecutor;
import org.testng.internal.thread.graph.IThreadWorkerFactory;
import org.testng.internal.thread.graph.SuiteWorkerFactory;
import org.testng.junit.JUnitTestFinder;
import org.testng.log4testng.Logger;
import org.testng.remote.SuiteDispatcher;
import org.testng.remote.SuiteSlave;
import org.testng.reporters.EmailableReporter;
import org.testng.reporters.EmailableReporter2;
import org.testng.reporters.FailedReporter;
import org.testng.reporters.JUnitReportReporter;
import org.testng.reporters.SuiteHTMLReporter;
import org.testng.reporters.VerboseReporter;
import org.testng.reporters.XMLReporter;
import org.testng.reporters.jq.Main;
import org.testng.xml.Parser;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlMethodSelector;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.xml.sax.SAXException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * This class is the main entry point for running tests in the TestNG framework.
 * Users can create their own TestNG object and invoke it in many different
 * ways:
 * <ul>
 * <li>On an existing testng.xml
 * <li>On a synthetic testng.xml, created entirely from Java
 * <li>By directly setting the test classes
 * </ul>
 * You can also define which groups to include or exclude, assign parameters,
 * etc...
 * <P/>
 * The command line parameters are:
 * <UL>
 * <LI>-d <TT>outputdir</TT>: specify the output directory</LI>
 * <LI>-testclass <TT>class_name</TT>: specifies one or several class names</li>
 * <LI>-testjar <TT>jar_name</TT>: specifies the jar containing the tests</LI>
 * <LI>-sourcedir <TT>src1;src2</TT>: ; separated list of source directories
 * (used only when javadoc annotations are used)</LI>
 * <LI>-target</LI>
 * <LI>-groups</LI>
 * <LI>-testrunfactory</LI>
 * <LI>-listener</LI>
 * </UL>
 * <P/>
 * Please consult documentation for more details.
 * 
 * FIXME: should support more than simple paths for suite xmls
 * 
 * @see #usage()
 * 
 * @author <a href = "mailto:cedric&#64;beust.com">Cedric Beust</a>
 * @author <a href = "mailto:the_mindstorm&#64;evolva.ro">Alex Popescu</a>
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class TestNG {

	/** This class' log4testng Logger. */
	private static final Logger LOGGER = Logger.getLogger(TestNG.class);

	/** The default name for a suite launched from the command line */
	public static final String DEFAULT_COMMAND_LINE_SUITE_NAME = "Command line suite";

	/** The default name for a test launched from the command line */
	public static final String DEFAULT_COMMAND_LINE_TEST_NAME = "Command line test";

	/**
	 * The default name of the result's output directory (keep public, used by
	 * Eclipse).
	 */
	public static final String DEFAULT_OUTPUTDIR = "test-output";

	/** System properties */
	public static final String SHOW_TESTNG_STACK_FRAMES = "testng.show.stack.frames";
	public static final String TEST_CLASSPATH = "testng.test.classpath";

	// 保存当前实例。init方法中赋值。
	private static TestNG m_instance;

	// 保存命令行传入参数信息。启动时候由privateMain方法初始化该值。
	private static JCommander m_jCommander;

	private List<String> m_commandLineMethods;

	protected List<XmlSuite> m_suites = Lists.newArrayList();
	private List<XmlSuite> m_cmdlineSuites;
	private String m_outputDir = DEFAULT_OUTPUTDIR;

	private String[] m_includedGroups;
	private String[] m_excludedGroups;

	private Boolean m_isJUnit = XmlSuite.DEFAULT_JUNIT;	// 这两个参数到底想表达什么
	private Boolean m_isMixed = XmlSuite.DEFAULT_MIXED;

	// useDefaultListeners 是否使用默认的listener。 init()默认调用过来传入true。
	protected boolean m_useDefaultListeners = true;

	private ITestRunnerFactory m_testRunnerFactory;

	// These listeners can be overridden from the command line
	private List<ITestListener> m_testListeners = Lists.newArrayList();
	private List<ISuiteListener> m_suiteListeners = Lists.newArrayList();
	
	// 报表列表
	private Set<IReporter> m_reporters = Sets.newHashSet();

	protected static final int HAS_FAILURE = 1;
	protected static final int HAS_SKIPPED = 2;
	protected static final int HAS_FSP = 4;
	protected static final int HAS_NO_TEST = 8;

	public static final Integer DEFAULT_VERBOSE = 1;

	private int m_status;
	private boolean m_hasTests = false;

	private String m_slavefileName = null;
	private String m_masterfileName = null;

	// Command line suite parameters
	private int m_threadCount;
	private boolean m_useThreadCount;
	private String m_parallelMode;
	private boolean m_useParallelMode;
	private String m_configFailurePolicy;
	private Class[] m_commandLineTestClasses;

	private String m_defaultSuiteName = DEFAULT_COMMAND_LINE_SUITE_NAME;
	private String m_defaultTestName = DEFAULT_COMMAND_LINE_TEST_NAME;

	private Map<String, Integer> m_methodDescriptors = Maps.newHashMap();

	private ITestObjectFactory m_objectFactory;

	private List<IInvokedMethodListener> m_invokedMethodListeners = Lists
			.newArrayList();

	private Integer m_dataProviderThreadCount = null;

	private String m_jarPath;
	/** The path of the testng.xml file inside the jar file */
	private String m_xmlPathInJar = CommandLineArgs.XML_PATH_IN_JAR_DEFAULT;

	// 初始化TestNG套件列表(根据命令行参数解析而来)
	private List<String> m_stringSuites = Lists.newArrayList();

	private IHookable m_hookable;
	private IConfigurable m_configurable;

	protected long m_end;
	protected long m_start;

	private List<IExecutionListener> m_executionListeners = Lists
			.newArrayList();

	// initializeSuitesAndJarFile方法来标记这个参数。
	private boolean m_isInitialized = false;

	/**
	 * Default constructor. Setting also usage of default listeners/reporters.
	 */
	public TestNG() {
		init(true);
	}

	/**
	 * Used by maven2 to have 0 output of any kind come out of testng.
	 * 
	 * @param useDefaultListeners
	 *            Whether or not any default reports should be added to tests.
	 */
	public TestNG(boolean useDefaultListeners) {
		init(useDefaultListeners);
	}

	private void init(boolean useDefaultListeners) {
		// 默认保持当前实例
		m_instance = this;

		// useDefaultListeners 是否使用默认的listener。 默认调用过来传入true。
		m_useDefaultListeners = useDefaultListeners;
		
		// Configuration初始化. 实际就是初始化了其中的annotationFinder(注解搜寻器)。
		m_configuration = new Configuration();
	}

	public int getStatus() {
		return m_status;
	}
	
	/**
	 * 用或来累加状态
	 * 
	 * @param status
	 */
	private void setStatus(int status) {
		m_status |= status;
	}

	/**
	 * Sets the output directory where the reports will be created.
	 * 
	 * @param outputdir
	 *            The directory.
	 */
	public void setOutputDirectory(final String outputdir) {
		if (isStringNotEmpty(outputdir)) {
			m_outputDir = outputdir;
		}
	}

	/**
	 * If this method is passed true before run(), the default listeners will
	 * not be used.
	 * <ul>
	 * <li>org.testng.reporters.TestHTMLReporter
	 * <li>org.testng.reporters.JUnitXMLReporter
	 * <li>org.testng.reporters.XMLReporter
	 * </ul>
	 * 
	 * @see org.testng.reporters.TestHTMLReporter
	 * @see org.testng.reporters.JUnitXMLReporter
	 * @see org.testng.reporters.XMLReporter
	 */
	public void setUseDefaultListeners(boolean useDefaultListeners) {
		m_useDefaultListeners = useDefaultListeners;
	}

	/**
	 * Sets a jar containing a testng.xml file.
	 * 
	 * @param jarPath
	 */
	public void setTestJar(String jarPath) {
		m_jarPath = jarPath;
	}

	/**
	 * Sets the path to the XML file in the test jar file.
	 */
	public void setXmlPathInJar(String xmlPathInJar) {
		m_xmlPathInJar = xmlPathInJar;
	}

	public void initializeSuitesAndJarFile() {
		// 避免多次调用该方法
		// The Eclipse plug-in (RemoteTestNG) might have invoked this method
		// already
		// so don't initialize suites twice.
		if (m_isInitialized) {
			return;
		}

		// 还没标记过，标记
		m_isInitialized = true;
		
		// m_suites不一定在每个单元测试都有的 先略过 TODO 
		if (m_suites.size() > 0) {
			// to parse the suite files (<suite-file>), if any
			for (XmlSuite s : m_suites) {
				List<String> suiteFiles = s.getSuiteFiles();
				for (int i = 0; i < suiteFiles.size(); i++) {
					try {
						Collection<XmlSuite> childSuites = getParser(
								suiteFiles.get(i)).parse();
						for (XmlSuite cSuite : childSuites) {
							cSuite.setParentSuite(s);
							s.getChildSuites().add(cSuite);
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace(System.out);
					} catch (ParserConfigurationException e) {
						e.printStackTrace(System.out);
					} catch (SAXException e) {
						e.printStackTrace(System.out);
					} catch (IOException e) {
						e.printStackTrace(System.out);
					}
				}

			}
			return;
		}

		//
		// m_stringSuites是肯定有的。
		// Parse the suites that were passed on the command line
		//
		for (String suitePath : m_stringSuites) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("suiteXmlPath: \"" + suitePath + "\"");
			}
			try {
				// 具体是如何解析的我不是太关心
				Collection<XmlSuite> allSuites = getParser(suitePath).parse();

				for (XmlSuite s : allSuites) {
					// If test names were specified, only run these test names
					if (m_testNames != null) {   // 这个是什么时候初始化的?
						m_suites.add(extractTestNames(s, m_testNames));
					} else {
						m_suites.add(s);		  // m_suites更新
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace(System.out);
			} catch (IOException e) {
				e.printStackTrace(System.out);
			} catch (ParserConfigurationException e) {
				e.printStackTrace(System.out);
			} catch (SAXException e) {
				e.printStackTrace(System.out);
			} catch (Exception ex) {
				// Probably a Yaml exception, unnest it
				Throwable t = ex;
				while (t.getCause() != null)
					t = t.getCause();
				// t.printStackTrace();
				if (t instanceof TestNGException)
					throw (TestNGException) t;
				else
					throw new TestNGException(t);
			}
		}

		//
		// jar path 命令行传入的suites优先于jar中的suite。 TODO(需要进一步理解)
		//
		// If suites were passed on the command line, they take precedence over
		// the suite file
		// inside that jar path
		if (m_jarPath != null && m_stringSuites.size() > 0) {
			StringBuilder suites = new StringBuilder();
			for (String s : m_stringSuites) {
				suites.append(s);
			}
			Utils.log("TestNG", 2, "Ignoring the XML file inside " + m_jarPath
					+ " and using " + suites + " instead");
			return;
		}
		if (isStringEmpty(m_jarPath)) {
			return;
		}

		// We have a jar file and no XML file was specified: try to find an XML
		// file inside the jar  感觉m_jarPath有东西但是命令行没传入suite才会走到这里? TODO
		File jarFile = new File(m_jarPath);

		try {

			Utils.log("TestNG", 2, "Trying to open jar file:" + jarFile);

			JarFile jf = new JarFile(jarFile);
			// System.out.println("   result: " + jf);
			Enumeration<JarEntry> entries = jf.entries();
			List<String> classes = Lists.newArrayList();
			boolean foundTestngXml = false;
			while (entries.hasMoreElements()) {
				JarEntry je = entries.nextElement();
				if (je.getName().equals(m_xmlPathInJar)) {
					Parser parser = getParser(jf.getInputStream(je));
					m_suites.addAll(parser.parse());
					foundTestngXml = true;
					break;
				} else if (je.getName().endsWith(".class")) {
					int n = je.getName().length() - ".class".length();
					classes.add(je.getName().replace("/", ".").substring(0, n));
				}
			}
			if (!foundTestngXml) {
				Utils.log("TestNG", 1, "Couldn't find the " + m_xmlPathInJar
						+ " in the jar file, running all the classes");
				XmlSuite xmlSuite = new XmlSuite();
				xmlSuite.setVerbose(0);
				xmlSuite.setName("Jar suite");
				XmlTest xmlTest = new XmlTest(xmlSuite);
				List<XmlClass> xmlClasses = Lists.newArrayList();
				for (String cls : classes) {
					XmlClass xmlClass = new XmlClass(cls);
					xmlClasses.add(xmlClass);
				}
				xmlTest.setXmlClasses(xmlClasses);
				m_suites.add(xmlSuite);
			}
		} catch (ParserConfigurationException ex) {
			ex.printStackTrace();
		} catch (SAXException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private Parser getParser(String path) {
		Parser result = new Parser(path);
		initProcessor(result);
		return result;
	}

	private Parser getParser(InputStream is) {
		Parser result = new Parser(is);
		initProcessor(result);
		return result;
	}

	private void initProcessor(Parser result) {
		result.setPostProcessor(new OverrideProcessor(m_includedGroups,
				m_excludedGroups));
	}

	/**
	 * If the XmlSuite contains at least one test named as testNames, return an
	 * XmlSuite that's made only of these tests, otherwise, return the original
	 * suite.
	 */
	private static XmlSuite extractTestNames(XmlSuite s, List<String> testNames) {
		List<XmlTest> tests = Lists.newArrayList();
		for (XmlTest xt : s.getTests()) {
			for (String tn : testNames) {
				if (xt.getName().equals(tn)) {
					tests.add(xt);
				}
			}
		}

		if (tests.size() == 0) {
			return s;
		} else {
			XmlSuite result = (XmlSuite) s.clone();
			result.getTests().clear();
			result.getTests().addAll(tests);
			return result;
		}
	}

	/**
	 * Define the number of threads in the thread pool.
	 */
	public void setThreadCount(int threadCount) {
		if (threadCount < 1) {
			exitWithError("Cannot use a threadCount parameter less than 1; 1 > "
					+ threadCount);
		}

		m_threadCount = threadCount;
		m_useThreadCount = true;
	}

	/**
	 * Define whether this run will be run in parallel mode.
	 */
	public void setParallel(String parallel) {
		m_parallelMode = parallel;
		m_useParallelMode = true;
	}

	public void setCommandLineSuite(XmlSuite suite) {
		m_cmdlineSuites = Lists.newArrayList();
		m_cmdlineSuites.add(suite);
		m_suites.add(suite);
	}

	/**
	 * Set the test classes to be run by this TestNG object. This method will
	 * create a dummy suite that will wrap these classes called
	 * "Command Line Test".
	 * <p/>
	 * If used together with threadCount, parallel, groups, excludedGroups than
	 * this one must be set first.
	 * 
	 * @param classes
	 *            An array of classes that contain TestNG annotations.
	 */
	public void setTestClasses(Class[] classes) {
		m_suites.clear();
		m_commandLineTestClasses = classes;
	}

	/**
	 * Given a string com.example.Foo.f1, return an array where [0] is the class
	 * and [1] is the method.
	 */
	private String[] splitMethod(String m) {
		int index = m.lastIndexOf(".");
		if (index < 0) {
			throw new TestNGException("Bad format for command line method:" + m
					+ ", expected <class>.<method>");
		}

		return new String[] { m.substring(0, index),
				m.substring(index + 1).replaceAll("\\*", "\\.\\*") };
	}

	/**
	 * @return a list of XmlSuite objects that represent the list of classes and
	 *         methods passed in parameter.
	 * 
	 * @param commandLineMethods
	 *            a string with the form "com.example.Foo.f1,com.example.Bar.f2"
	 */
	private List<XmlSuite> createCommandLineSuitesForMethods(
			List<String> commandLineMethods) {
		//
		// Create the <classes> tag
		//
		Set<Class> classes = Sets.newHashSet();
		for (String m : commandLineMethods) {
			Class c = ClassHelper.forName(splitMethod(m)[0]);
			if (c != null) {
				classes.add(c);
			}
		}

		List<XmlSuite> result = createCommandLineSuitesForClasses(classes
				.toArray(new Class[0]));

		//
		// Add the method tags
		//
		List<XmlClass> xmlClasses = Lists.newArrayList();
		for (XmlSuite s : result) {
			for (XmlTest t : s.getTests()) {
				xmlClasses.addAll(t.getClasses());
			}
		}

		for (XmlClass xc : xmlClasses) {
			for (String m : commandLineMethods) {
				String[] split = splitMethod(m);
				String className = split[0];
				if (xc.getName().equals(className)) {
					XmlInclude includedMethod = new XmlInclude(split[1]);
					xc.getIncludedMethods().add(includedMethod);
				}
			}
		}

		return result;
	}

	private List<XmlSuite> createCommandLineSuitesForClasses(Class[] classes) {
		//
		// See if any of the classes has an xmlSuite or xmlTest attribute.
		// If it does, create the appropriate XmlSuite, otherwise, create
		// the default one
		//
		XmlClass[] xmlClasses = Utils.classesToXmlClasses(classes);
		Map<String, XmlSuite> suites = Maps.newHashMap();
		IAnnotationFinder finder = m_configuration.getAnnotationFinder();

		for (int i = 0; i < classes.length; i++) {
			Class c = classes[i];
			ITestAnnotation test = finder.findAnnotation(c,
					ITestAnnotation.class);
			String suiteName = getDefaultSuiteName();
			String testName = getDefaultTestName();
			boolean isJUnit = false;
			if (test != null) {
				suiteName = defaultIfStringEmpty(test.getSuiteName(), suiteName);
				testName = defaultIfStringEmpty(test.getTestName(), testName);
			} else {
				if (m_isMixed && JUnitTestFinder.isJUnitTest(c)) {
					isJUnit = true;
					testName = c.getName();
				}
			}
			XmlSuite xmlSuite = suites.get(suiteName);
			if (xmlSuite == null) {
				xmlSuite = new XmlSuite();
				xmlSuite.setName(suiteName);
				suites.put(suiteName, xmlSuite);
			}

			if (m_dataProviderThreadCount != null) {
				xmlSuite.setDataProviderThreadCount(m_dataProviderThreadCount);
			}
			XmlTest xmlTest = null;
			for (XmlTest xt : xmlSuite.getTests()) {
				if (xt.getName().equals(testName)) {
					xmlTest = xt;
					break;
				}
			}

			if (xmlTest == null) {
				xmlTest = new XmlTest(xmlSuite);
				xmlTest.setName(testName);
				xmlTest.setJUnit(isJUnit);
			}

			xmlTest.getXmlClasses().add(xmlClasses[i]);
		}

		return new ArrayList<XmlSuite>(suites.values());
	}

	public void addMethodSelector(String className, int priority) {
		m_methodDescriptors.put(className, priority);
	}

	/**
	 * Set the suites file names to be run by this TestNG object. This method
	 * tries to load and parse the specified TestNG suite xml files. If a file
	 * is missing, it is ignored.
	 * 
	 * @param suites
	 *            A list of paths to one more XML files defining the tests. For
	 *            example:
	 * 
	 *            <pre>
	 * TestNG tng = new TestNG();
	 * List&lt;String&gt; suites = Lists.newArrayList();
	 * suites.add(&quot;c:/tests/testng1.xml&quot;);
	 * suites.add(&quot;c:/tests/testng2.xml&quot;);
	 * tng.setTestSuites(suites);
	 * tng.run();
	 * </pre>
	 */
	public void setTestSuites(List<String> suites) {
		m_stringSuites = suites;
	}

	/**
	 * Specifies the XmlSuite objects to run.
	 * 
	 * @param suites
	 * @see org.testng.xml.XmlSuite
	 */
	public void setXmlSuites(List<XmlSuite> suites) {
		m_suites = suites;
	}

	/**
	 * Define which groups will be excluded from this run.
	 * 
	 * @param groups
	 *            A list of group names separated by a comma.
	 */
	public void setExcludedGroups(String groups) {
		m_excludedGroups = Utils.split(groups, ",");
	}

	/**
	 * Define which groups will be included from this run.
	 * 
	 * @param groups
	 *            A list of group names separated by a comma.
	 */
	public void setGroups(String groups) {
		m_includedGroups = Utils.split(groups, ",");
	}

	private void setTestRunnerFactoryClass(Class testRunnerFactoryClass) {
		setTestRunnerFactory((ITestRunnerFactory) ClassHelper
				.newInstance(testRunnerFactoryClass));
	}

	protected void setTestRunnerFactory(ITestRunnerFactory itrf) {
		m_testRunnerFactory = itrf;
	}

	public void setObjectFactory(Class c) {
		m_objectFactory = (ITestObjectFactory) ClassHelper.newInstance(c);
	}

	public void setObjectFactory(ITestObjectFactory factory) {
		m_objectFactory = factory;
	}

	/**
	 * Define which listeners to user for this run.
	 * 
	 * @param classes
	 *            A list of classes, which must be either ISuiteListener,
	 *            ITestListener or IReporter
	 */
	public void setListenerClasses(List<Class> classes) {
		for (Class cls : classes) {
			addListener(ClassHelper.newInstance(cls));
		}
	}

	public void addListener(Object listener) {
		if (!(listener instanceof ITestNGListener)) {
			exitWithError("Listener "
					+ listener
					+ " must be one of ITestListener, ISuiteListener, IReporter, "
					+ " IAnnotationTransformer, IMethodInterceptor or IInvokedMethodListener");
		} else {
			if (listener instanceof ISuiteListener) {
				addListener((ISuiteListener) listener);
			}
			if (listener instanceof ITestListener) {
				addListener((ITestListener) listener);
			}
			if (listener instanceof IReporter) {
				addListener((IReporter) listener);
			}
			if (listener instanceof IAnnotationTransformer) {
				setAnnotationTransformer((IAnnotationTransformer) listener);
			}
			if (listener instanceof IMethodInterceptor) {
				setMethodInterceptor((IMethodInterceptor) listener);
			}
			if (listener instanceof IInvokedMethodListener) {
				addInvokedMethodListener((IInvokedMethodListener) listener);
			}
			if (listener instanceof IHookable) {
				setHookable((IHookable) listener);
			}
			if (listener instanceof IConfigurable) {
				setConfigurable((IConfigurable) listener);
			}
			if (listener instanceof IExecutionListener) {
				addExecutionListener((IExecutionListener) listener);
			}
			if (listener instanceof IConfigurationListener) {
				getConfiguration().addConfigurationListener(
						(IConfigurationListener) listener);
			}
		}
	}

	public void addListener(IInvokedMethodListener listener) {
		m_invokedMethodListeners.add(listener);
	}

	public void addListener(ISuiteListener listener) {
		if (null != listener) {
			m_suiteListeners.add(listener);
		}
	}

	public void addListener(ITestListener listener) {
		if (null != listener) {
			m_testListeners.add(listener);
		}
	}

	public void addListener(IReporter listener) {
		if (null != listener) {
			m_reporters.add(listener);
		}
	}

	public void addInvokedMethodListener(IInvokedMethodListener listener) {
		m_invokedMethodListeners.add(listener);
	}

	public Set<IReporter> getReporters() {
		return m_reporters;
	}

	public List<ITestListener> getTestListeners() {
		return m_testListeners;
	}

	public List<ISuiteListener> getSuiteListeners() {
		return m_suiteListeners;
	}

	/**
	 * If m_verbose gets set, it will override the verbose setting in testng.xml
	 */
	private Integer m_verbose = null;

	// TestNG实例化时初始化注释转换器
	private final IAnnotationTransformer m_defaultAnnoProcessor = new DefaultAnnotationTransformer();
	
	// 初始化时持有注释转换器。
	private IAnnotationTransformer m_annotationTransformer = m_defaultAnnoProcessor;

	private Boolean m_skipFailedInvocationCounts = false;

	private IMethodInterceptor m_methodInterceptor = null;

	/** The list of test names to run from the given suite */
	private List<String> m_testNames;

	private Integer m_suiteThreadPoolSize = CommandLineArgs.SUITE_THREAD_POOL_SIZE_DEFAULT;

	private boolean m_randomizeSuites = Boolean.FALSE;

	private boolean m_preserveOrder = false;
	private Boolean m_groupByInstances;

	// 核心的配置管理接口
	private IConfiguration m_configuration;

	/**
	 * Sets the level of verbosity. This value will override the value specified
	 * in the test suites.
	 * 
	 * @param verbose
	 *            the verbosity level (0 to 10 where 10 is most detailed)
	 *            Actually, this is a lie: you can specify -1 and this will put
	 *            TestNG in debug mode (no longer slicing off stack traces and
	 *            all).
	 */
	public void setVerbose(int verbose) {
		m_verbose = verbose;
	}

	private void initializeCommandLineSuites() {
		if (m_commandLineTestClasses != null || m_commandLineMethods != null) {
			if (null != m_commandLineMethods) {
				m_cmdlineSuites = createCommandLineSuitesForMethods(m_commandLineMethods);
			} else {
				m_cmdlineSuites = createCommandLineSuitesForClasses(m_commandLineTestClasses);
			}

			for (XmlSuite s : m_cmdlineSuites) {
				for (XmlTest t : s.getTests()) {
					t.setPreserveOrder(m_preserveOrder ? "true " : "false");
				}
				m_suites.add(s);
				if (m_groupByInstances != null) {
					s.setGroupByInstances(m_groupByInstances);
				}
			}
		}
	}

	private void initializeCommandLineSuitesParams() {
		if (null == m_cmdlineSuites) {
			return;
		}

		for (XmlSuite s : m_cmdlineSuites) {
			if (m_useThreadCount) {
				s.setThreadCount(m_threadCount);
			}
			if (m_useParallelMode) {
				s.setParallel(m_parallelMode);
			}
			if (m_configFailurePolicy != null) {
				s.setConfigFailurePolicy(m_configFailurePolicy.toString());
			}
		}

	}

	private void initializeCommandLineSuitesGroups() {
		// If groups were specified on the command line, they should override
		// groups
		// specified in the XML file
		boolean hasIncludedGroups = null != m_includedGroups
				&& m_includedGroups.length > 0;
		boolean hasExcludedGroups = null != m_excludedGroups
				&& m_excludedGroups.length > 0;
		List<XmlSuite> suites = m_cmdlineSuites != null ? m_cmdlineSuites
				: m_suites;
		if (hasIncludedGroups || hasExcludedGroups) {
			for (XmlSuite s : suites) {
				// set on each test, instead of just the first one of the suite
				for (XmlTest t : s.getTests()) {
					if (hasIncludedGroups) {
						t.setIncludedGroups(Arrays.asList(m_includedGroups));
					}
					if (hasExcludedGroups) {
						t.setExcludedGroups(Arrays.asList(m_excludedGroups));
					}
				}
			}
		}
	}

	/**
	 * 为什么这里需要实例的方式添加? 不是直接new对象呢 擦。TODO
	 * 
	 * @param r
	 */
	private void addReporter(Class<? extends IReporter> r) {
		if (!m_reporters.contains(r)) {
			m_reporters.add(ClassHelper.newInstance(r));
		}
	}

	/**
	 * 添加异常错误监听器，添加报表信息
	 */
	private void initializeDefaultListeners() {
		// 初始化了一个异常错误的监听器，并添加到TestNG中
		m_testListeners.add(new ExitCodeListener(this));

		// 是否使用默认的Listener，默认是true。
		if (m_useDefaultListeners) {
			// 添加6种报表类型
			addReporter(SuiteHTMLReporter.class);
			addReporter(Main.class);
			addReporter(FailedReporter.class);
			addReporter(XMLReporter.class);
			
			// 参数oldDestngEmailableReporter用来指定是否使用老的邮件报表
			if (System.getProperty("oldDestngEmailableReporter") != null) {
				addReporter(EmailableReporter.class);
			} else {
				addReporter(EmailableReporter2.class);
			}
			addReporter(JUnitReportReporter.class);
			
			// 初始化的时候m_verbose是null
			if (m_verbose != null && m_verbose > 4) {
				addListener(new VerboseReporter("[TestNG] "));
			}
		}
	}

	private void initializeConfiguration() {
		ITestObjectFactory factory = m_objectFactory;    // 初始化时候objectFactory为空
		
		//
		// 不明白这里，需要关注
		// Install the listeners found in ServiceLoader (or use the class
		// loader for tests, if specified).
		//
		addServiceLoaderListeners();

		//
		// 这里在第一步已经安装了一个suites在里边，可循环
		// 但是如果跑的是最简单的例子，这个循环等于就没跑
		// Install the listeners found in the suites 
		//
		for (XmlSuite s : m_suites) {
			for (String listenerName : s.getListeners()) {	// listener默认是没有的
				Class<?> listenerClass = ClassHelper.forName(listenerName);

				// If specified listener does not exist, a TestNGException will
				// be thrown
				if (listenerClass == null) {
					throw new TestNGException("Listener " + listenerName
							+ " was not found in project's classpath");
				}

				Object listener = ClassHelper.newInstance(listenerClass);
				addListener(listener);
			}

			// getMethodSelectors默认也是没有的
			// Install the method selectors 
			//
			for (XmlMethodSelector methodSelector : s.getMethodSelectors()) {
				addMethodSelector(methodSelector.getClassName(),
						methodSelector.getPriority());
			}

			//
			// Find if we have an object factory
			//
			if (s.getObjectFactory() != null) {
				if (factory == null) {
					factory = s.getObjectFactory();
				} else {
					throw new TestNGException(
							"Found more than one object-factory tag in your suites");
				}
			}
		}
		
		// new出来一个注解搜寻器并设置到核心的配置管理接口。
		m_configuration.setAnnotationFinder(new JDK15AnnotationFinder(getAnnotationTransformer()));
		m_configuration.setHookable(m_hookable);			// 最简单的例子的时候这里是空
		m_configuration.setConfigurable(m_configurable);	// 最简单的例子的时候这里是空
		m_configuration.setObjectFactory(factory);			// 最简单的例子的时候这里是空
	}

	/**
	 * 不明白这里 TODO 想要加载什么东西?
	 * 
	 * Using reflection to remain Java 5 compliant.
	 */
	private void addServiceLoaderListeners() {
		try {
			Class c = Class.forName("java.util.ServiceLoader");
			List<Object> parameters = Lists.newArrayList();
			parameters.add(ITestNGListener.class);
			Method loadMethod;
			if (m_serviceLoaderClassLoader != null) {
				parameters.add(m_serviceLoaderClassLoader);
				loadMethod = c
						.getMethod("load", Class.class, ClassLoader.class);
			} else {
				loadMethod = c.getMethod("load", Class.class);
			}
			Iterable<ITestNGListener> loader = (Iterable<ITestNGListener>) loadMethod
					.invoke(c, parameters.toArray());
			// Object loader = c.
			// ServiceLoader<ITestNGListener> loader =
			// m_serviceLoaderClassLoader != null
			// ? ServiceLoader.load(ITestNGListener.class,
			// m_serviceLoaderClassLoader)
			// : ServiceLoader.load(ITestNGListener.class);
			for (ITestNGListener l : loader) {
				Utils.log("[TestNG]", 2, "Adding ServiceLoader listener:" + l);
				addListener(l);
				addServiceLoaderListener(l);
			}
		} catch (ClassNotFoundException ex) {
			// Ignore
		} catch (NoSuchMethodException ex) {
			// Ignore
		} catch (IllegalAccessException ex) {
			// Ignore
		} catch (InvocationTargetException ex) {
			// Ignore
		}
	}

	/**
	 * 准备执行单元测试前做一遍完整性检查。保证需要的环境和数据都准备好了。
	 * 如果没准备好，会抛出TestNGException异常。
	 * 
	 * Before suites are executed, do a sanity check to ensure all required
	 * conditions are met. If not, throw an exception to stop test execution
	 * 
	 * @throws TestNGException
	 *             if the sanity check fails
	 */
	private void sanityCheck() {
		// 保证suite和test的name是唯一的。 TODO
		// 后边还是要关注下从xml解析到XmlSuite的过程。
		checkTestNames(m_suites);
		checkSuiteNames(m_suites);
	}

	/**
	 * Ensure that two XmlTest within the same XmlSuite don't have the same name
	 */
	private void checkTestNames(List<XmlSuite> suites) {
		for (XmlSuite suite : suites) {
			Set<String> testNames = Sets.newHashSet();
			for (XmlTest test : suite.getTests()) {
				if (testNames.contains(test.getName())) {
					throw new TestNGException("Two tests in the same suite "
							+ "cannot have the same name: " + test.getName());
				} else {
					testNames.add(test.getName());
				}
			}
			checkTestNames(suite.getChildSuites());
		}
	}

	/**
	 * Ensure that two XmlSuite don't have the same name Otherwise will be clash
	 * in SuiteRunnerMap See issue #302
	 */
	private void checkSuiteNames(List<XmlSuite> suites) {
		checkSuiteNamesInternal(suites, Sets.<String> newHashSet());
	}

	private void checkSuiteNamesInternal(List<XmlSuite> suites,
			Set<String> names) {
		for (XmlSuite suite : suites) {
			final String name = suite.getName();
			if (names.contains(name)) {
				throw new TestNGException(
						"Two suites cannot have the same name: " + name);
			}
			names.add(name);
			checkSuiteNamesInternal(suite.getChildSuites(), names);
		}
	}

	/**
	 * TestNG正式启动。其实启动的方式有点像spring? 
	 * 
	 * Run TestNG.
	 */
	public void run() {
		// 初始化命令行传入的套件suites以及jar包内的suites
		initializeSuitesAndJarFile();
		
		// 在最简单的例子下，基本上没做什么事情。需要关注一下里边的那个反射调用到底在干吗。
		initializeConfiguration();
		
		// 添加异常错误监听器，添加报表信息
		initializeDefaultListeners();
		
		// 命令行套件添加，由于我们直接指定的是xml内容，所以这里什么都没做
		initializeCommandLineSuites();
		
		// 命令行套件参数初始化，由于我们直接指定的是xml内容，所以这里什么都没做
		initializeCommandLineSuitesParams();
		
		// 命令行套件组别初始化，由于我们直接指定的是xml内容，所以这里什么都没做
		initializeCommandLineSuitesGroups();

		// 准备执行单元测试前做一遍完整性检查。保证需要的环境和数据都准备好了。
		// 如果没准备好，会抛出异常。
		// 主要是判断XmlSuite中的名字是否唯一。
		sanityCheck();

		List<ISuite> suiteRunners = null;

		// 启动，什么都没做? 默认过去的list和listner都是空? TODO
		runExecutionListeners(true /* start */);

		m_start = System.currentTimeMillis();
 
		// TODO 这个值什么时候设置
		if (m_slavefileName != null) { 
			// Slave mode 
			SuiteSlave slave = new SuiteSlave(m_slavefileName, this);
			slave.waitForSuites();
		} else if (m_masterfileName == null) {
			// 这里是实际跑单元测试的地方  runSuitesLocally()方法
			// Regular mode  
			suiteRunners = runSuitesLocally();
		} else { 
			// Master mode 
			SuiteDispatcher dispatcher = new SuiteDispatcher(m_masterfileName);
			suiteRunners = dispatcher.dispatch(getConfiguration(), m_suites,
					getOutputDirectory(), getTestListeners());
		}

		m_end = System.currentTimeMillis();
		runExecutionListeners(false /* finish */);

		if (null != suiteRunners) {
			generateReports(suiteRunners);
		}

		if (!m_hasTests) {
			setStatus(HAS_NO_TEST);
			if (TestRunner.getVerbose() > 1) {
				System.err.println("[TestNG] No tests found. Nothing was run");
				usage();
			}
		}
	} 
	 
	/**
	 * 太搓B了， 用这个start参数来控制启动的正常和失败。
	 * 
	 * @param start
	 */
	private void runExecutionListeners(boolean start) {
		for (List<IExecutionListener> listeners : Arrays.asList(m_executionListeners, m_configuration.getExecutionListeners())) {
			for (IExecutionListener l : listeners) {
				if (start)
					l.onExecutionStart();
				else
					l.onExecutionFinish();
			}
		}
	}

	public void addExecutionListener(IExecutionListener l) {
		m_executionListeners.add(l);
	}

	private static void usage() {
		if (m_jCommander == null) {
			m_jCommander = new JCommander(new CommandLineArgs());
		}
		m_jCommander.usage();
	}

	private void generateReports(List<ISuite> suiteRunners) {
		for (IReporter reporter : m_reporters) {
			try {
				long start = System.currentTimeMillis();
				reporter.generateReport(m_suites, suiteRunners, m_outputDir);
				Utils.log("TestNG", 2, "Time taken by " + reporter + ": "
						+ (System.currentTimeMillis() - start) + " ms");
			} catch (Exception ex) {
				System.err.println("[TestNG] Reporter " + reporter + " failed");
				ex.printStackTrace(System.err);
			}
		}
	}

	/**
	 * 这个方法公开给Maven? maven插件以后调用这个方法就可以了? TODO
	 * 
	 * This needs to be public for maven2, for now..At least until an
	 * alternative mechanism is found.
	 */
	public List<ISuite> runSuitesLocally() {
		SuiteRunnerMap suiteRunnerMap = new SuiteRunnerMap();
		if (m_suites.size() > 0) {
			 // 默认的suite的verbose值是1. verbose代表什么含义? TODO
			if (m_suites.get(0).getVerbose() >= 2) {		
				Version.displayBanner();
			}

			// 实例化对象，TestNG相关实例初始化 
			//
			// First initialize the suite runners to ensure there are no
			// configuration issues.
			// Create a map with XmlSuite as key and corresponding SuiteRunner
			// as value
			for (XmlSuite xmlSuite : m_suites) {    
				createSuiteRunners(suiteRunnerMap, xmlSuite);
			}

			// m_suiteThreadPoolSize默认是1(可以命令行传入进行改变)
			// m_randomizeSuites默认是false。
			// Run suites
			//
			if (m_suiteThreadPoolSize == 1 && !m_randomizeSuites) {
				// Single threaded and not randomized: run the suites in order
				for (XmlSuite xmlSuite : m_suites) {			// 这个循环是实际执行用例的地方!
					runSuitesSequentially(
							xmlSuite,
							suiteRunnerMap,
							getVerbose(xmlSuite),
							getDefaultSuiteName());
				}
			} else {
				// Multithreaded: generate a dynamic graph that stores the suite
				// hierarchy. This is then
				// used to run related suites in specific order. Parent suites
				// are run only
				// once all the child suites have completed execution
				DynamicGraph<ISuite> suiteGraph = new DynamicGraph<ISuite>();
				for (XmlSuite xmlSuite : m_suites) {
					populateSuiteGraph(suiteGraph, suiteRunnerMap, xmlSuite);
				}

				IThreadWorkerFactory<ISuite> factory = new SuiteWorkerFactory(
						suiteRunnerMap, 0 /* verbose hasn't been set yet */,
						getDefaultSuiteName());
				GraphThreadPoolExecutor<ISuite> pooledExecutor = new GraphThreadPoolExecutor<ISuite>(
						suiteGraph, factory, m_suiteThreadPoolSize,
						m_suiteThreadPoolSize, Integer.MAX_VALUE,
						TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<Runnable>());

				Utils.log("TestNG", 2, "Starting executor for all suites");
				// Run all suites in parallel
				pooledExecutor.run();
				try {
					pooledExecutor.awaitTermination(Long.MAX_VALUE,
							TimeUnit.MILLISECONDS);
					pooledExecutor.shutdownNow();
				} catch (InterruptedException handled) {
					Thread.currentThread().interrupt();
					error("Error waiting for concurrent executors to finish "
							+ handled.getMessage());
				}
			}
		} else {
			setStatus(HAS_NO_TEST);
			error("No test suite found. Nothing to run");
			usage();
		}

		//
		// Generate the suites report
		//
		return Lists.newArrayList(suiteRunnerMap.values());
	}

	private static void error(String s) {
		LOGGER.error(s);
	}

	/**
	 * @return the verbose level, checking in order: the verbose level on the
	 *         suite, the verbose level on the TestNG object, or 1.
	 */
	private int getVerbose(XmlSuite xmlSuite) {
		int result = xmlSuite.getVerbose() != null ? xmlSuite.getVerbose()
				: (m_verbose != null ? m_verbose : DEFAULT_VERBOSE);
		return result;
	}

	/**
	 * Recursively runs suites. Runs the children suites before running the
	 * parent suite. This is done so that the results for parent suite can
	 * reflect the combined results of the children suites.
	 * 
	 * @param xmlSuite
	 *            XML Suite to be executed
	 * @param suiteRunnerMap
	 *            Maps {@code XmlSuite}s to respective {@code ISuite}
	 * @param verbose
	 *            verbose level
	 * @param defaultSuiteName
	 *            default suite name
	 */
	private void runSuitesSequentially(
			XmlSuite xmlSuite,
			SuiteRunnerMap suiteRunnerMap, 
			int verbose, 
			String defaultSuiteName) {
		// 最简单的例子里边xmlSuite.getChildSuites()是空集合，所以会跳过
		for (XmlSuite childSuite : xmlSuite.getChildSuites()) {
			runSuitesSequentially(
					childSuite, 
					suiteRunnerMap,
					verbose,
					defaultSuiteName);
		}
		
		SuiteRunnerWorker srw = new SuiteRunnerWorker(
				suiteRunnerMap.get(xmlSuite),
				suiteRunnerMap, 
				verbose,
				defaultSuiteName);
		srw.run();
	}

	/**
	 * Populates the dynamic graph with the reverse hierarchy of suites. Edges
	 * are added pointing from child suite runners to parent suite runners,
	 * hence making parent suite runners dependent on all the child suite
	 * runners
	 * 
	 * @param suiteGraph
	 *            dynamic graph representing the reverse hierarchy of
	 *            SuiteRunners
	 * @param suiteRunnerMap
	 *            Map with XMLSuite as key and its respective SuiteRunner as
	 *            value
	 * @param xmlSuite
	 *            XML Suite
	 */
	private void populateSuiteGraph(DynamicGraph<ISuite> suiteGraph /* OUT */,
			SuiteRunnerMap suiteRunnerMap, XmlSuite xmlSuite) {
		ISuite parentSuiteRunner = suiteRunnerMap.get(xmlSuite);
		if (xmlSuite.getChildSuites().isEmpty()) {
			suiteGraph.addNode(parentSuiteRunner);
		} else {
			for (XmlSuite childSuite : xmlSuite.getChildSuites()) {
				suiteGraph.addEdge(parentSuiteRunner,
						suiteRunnerMap.get(childSuite));
				populateSuiteGraph(suiteGraph, suiteRunnerMap, childSuite);
			}
		}
	}

	/**
	 * Creates the {@code SuiteRunner}s and populates the suite runner map with
	 * this information
	 * 
	 * @param suiteRunnerMap
	 *            Map with XMLSuite as key and it's respective SuiteRunner as
	 *            value. This is updated as part of this method call
	 * @param xmlSuite
	 *            Xml Suite (and its children) for which {@code SuiteRunner}s
	 *            are created
	 */
	private void createSuiteRunners(SuiteRunnerMap suiteRunnerMap /* OUT */,
			XmlSuite xmlSuite) {
		if (null != m_isJUnit && !m_isJUnit.equals(XmlSuite.DEFAULT_JUNIT)) {  // 非JUNIT 跳过 
			xmlSuite.setJUnit(m_isJUnit);
		}

		// If the skip flag was invoked on the command line, it
		// takes precedence 简单例子里边这里是空
		if (null != m_skipFailedInvocationCounts) {
			xmlSuite.setSkipFailedInvocationCounts(m_skipFailedInvocationCounts);
		}

		// Override the XmlSuite verbose value with the one from TestNG 简单例子里边这里是空
		if (m_verbose != null) {
			xmlSuite.setVerbose(m_verbose);
		}

		if (null != m_configFailurePolicy) {	//简单例子里边这里是空
			xmlSuite.setConfigFailurePolicy(m_configFailurePolicy);
		}

		for (XmlTest t : xmlSuite.getTests()) {  // m_methodDescriptors在简单例子里边是空，跳过
			for (Map.Entry<String, Integer> ms : m_methodDescriptors.entrySet()) {
				XmlMethodSelector xms = new XmlMethodSelector();
				xms.setName(ms.getKey());
				xms.setPriority(ms.getValue());
				t.getMethodSelectors().add(xms);
			}
		}

		suiteRunnerMap.put(xmlSuite, createSuiteRunner(xmlSuite));

		for (XmlSuite childSuite : xmlSuite.getChildSuites()) {
			createSuiteRunners(suiteRunnerMap, childSuite);
		}
	}

	/**
	 * Creates a suite runner and configures its initial state
	 * 
	 * @param xmlSuite
	 * @return returns the newly created suite runner
	 */
	private SuiteRunner createSuiteRunner(XmlSuite xmlSuite) {
		SuiteRunner result = new SuiteRunner(getConfiguration(), xmlSuite,
				m_outputDir, m_testRunnerFactory, m_useDefaultListeners,
				m_methodInterceptor, m_invokedMethodListeners, m_testListeners);

		for (ISuiteListener isl : m_suiteListeners) {
			result.addListener(isl);
		}

		for (IReporter r : result.getReporters()) {
			addListener(r);
		}

		for (IConfigurationListener cl : m_configuration
				.getConfigurationListeners()) {
			result.addListener(cl);
		}

		return result;
	}

	protected IConfiguration getConfiguration() {
		return m_configuration;
	}

	/**
	 * TestNG命令行切入点。go。
	 * 
	 * 场景1: 这里的argv就是suite对应的xml，比如我的例子中对应的是
	 * C:\Users\yingchao.zyc\AppData\Local\
	 * Temp\testng-eclipse--964453265\testng-customsuite.xml
	 * 
	 * The TestNG entry point for command line execution.
	 * 
	 * @param argv
	 *            the TestNG command line parameters.
	 * @throws FileNotFoundException
	 */
	public static void main(String[] argv) {
		TestNG testng = privateMain(argv, null);
		System.exit(testng.getStatus());
	}

	/**
	 * 官方注解，这个方法虽然是public的，但是不是一个供大家使用的共有api。 仅仅是内部使用。
	 * 
	 * 妈蛋，那你不写成私有的，草。
	 * 
	 * privateMain目前就TestNG的main方法在调用。 listener为空。
	 * 
	 * <B>Note</B>: this method is not part of the public API and is meant for
	 * internal usage only.
	 */
	public static TestNG privateMain(String[] argv, ITestListener listener) {
		// TestNG初始化
		// 注解转换器DefaultAnnotationTransformer初始化生成。
		// 注解搜寻器JDK15AnnotationFinder初始化生成。
		TestNG result = new TestNG();

		// TestNG开始启动的时候这个listener肯定是空。所以不走这里。
		// 什么时候不为空? TODO
		if (null != listener) {
			result.addListener(listener);
		}

		//
		// Parse the arguments(参数解析)
		//
		try {
			// 命令行参数解析对象，用到了开源组件JCommander
			CommandLineArgs cla = new CommandLineArgs();
			
			// 根据命令行传入参数解析为JCommander对象
			m_jCommander = new JCommander(cla, argv);
			
			// 校验命令行参数 TODO  
			// 这句不应该在m_jCommander = new JCommander(cla, argv)之前才对么
			validateCommandLineParameters(cla);
			
			// 根据传入的命令行参数配置TestNG。
			result.configure(cla);
		} catch (ParameterException ex) {
			exitWithError(ex.getMessage());
		}

		//
		// Run(最核心的启动部分)
		//
		try {
			result.run();
		} catch (TestNGException ex) {
			if (TestRunner.getVerbose() > 1) {
				ex.printStackTrace(System.out);
			} else {
				error(ex.getMessage());
			}
			result.setStatus(HAS_FAILURE);
		}

		return result;
	}

	/**
	 * Configure the TestNG instance based on the command line parameters.
	 */
	protected void configure(CommandLineArgs cla) {
		if (cla.verbose != null) {
			setVerbose(cla.verbose);
		}
		setOutputDirectory(cla.outputDirectory);

		String testClasses = cla.testClass;
		if (null != testClasses) {
			String[] strClasses = testClasses.split(",");
			List<Class> classes = Lists.newArrayList();
			for (String c : strClasses) {
				classes.add(ClassHelper.fileToClass(c));
			}

			setTestClasses(classes.toArray(new Class[classes.size()]));
		}

		setOutputDirectory(cla.outputDirectory);

		if (cla.testNames != null) {
			setTestNames(Arrays.asList(cla.testNames.split(",")));
		}

		// List<String> testNgXml = (List<String>)
		// cmdLineArgs.get(CommandLineArgs.SUITE_DEF);
		// if (null != testNgXml) {
		// setTestSuites(testNgXml);
		// }

		// Note: can't use a Boolean field here because we are allowing a
		// boolean
		// parameter with an arity of 1 ("-usedefaultlisteners false")
		if (cla.useDefaultListeners != null) {
			setUseDefaultListeners("true"
					.equalsIgnoreCase(cla.useDefaultListeners));
		}

		setGroups(cla.groups);
		setExcludedGroups(cla.excludedGroups);
		setTestJar(cla.testJar);
		setXmlPathInJar(cla.xmlPathInJar);
		setJUnit(cla.junit);
		setMixed(cla.mixed);
		setMaster(cla.master);
		setSlave(cla.slave);
		setSkipFailedInvocationCounts(cla.skipFailedInvocationCounts);
		if (cla.parallelMode != null) {
			setParallel(cla.parallelMode);
		}
		if (cla.configFailurePolicy != null) {
			setConfigFailurePolicy(cla.configFailurePolicy);
		}
		if (cla.threadCount != null) {
			setThreadCount(cla.threadCount);
		}
		if (cla.dataProviderThreadCount != null) {
			setDataProviderThreadCount(cla.dataProviderThreadCount);
		}
		if (cla.suiteName != null) {
			setDefaultSuiteName(cla.suiteName);
		}
		if (cla.testName != null) {
			setDefaultTestName(cla.testName);
		}
		if (cla.listener != null) {
			String sep = ";";
			if (cla.listener.indexOf(",") >= 0) {
				sep = ",";
			}
			String[] strs = Utils.split(cla.listener, sep);
			List<Class> classes = Lists.newArrayList();

			for (String cls : strs) {
				classes.add(ClassHelper.fileToClass(cls));
			}

			setListenerClasses(classes);
		}

		if (null != cla.methodSelectors) {
			String[] strs = Utils.split(cla.methodSelectors, ",");
			for (String cls : strs) {
				String[] sel = Utils.split(cls, ":");
				try {
					if (sel.length == 2) {
						addMethodSelector(sel[0], Integer.valueOf(sel[1]));
					} else {
						error("Method selector value was not in the format org.example.Selector:4");
					}
				} catch (NumberFormatException nfe) {
					error("Method selector value was not in the format org.example.Selector:4");
				}
			}
		}

		if (cla.objectFactory != null) {
			setObjectFactory(ClassHelper.fileToClass(cla.objectFactory));
		}
		if (cla.testRunnerFactory != null) {
			setTestRunnerFactoryClass(ClassHelper
					.fileToClass(cla.testRunnerFactory));
		}

		if (cla.reporter != null) {
			ReporterConfig reporterConfig = ReporterConfig
					.deserialize(cla.reporter);
			addReporter(reporterConfig);
		}

		if (cla.commandLineMethods.size() > 0) {
			m_commandLineMethods = cla.commandLineMethods;
		}
		
		// 一个最基本的配置，suiteFiles是必须有的。
		if (cla.suiteFiles != null) {
			setTestSuites(cla.suiteFiles);
		}

		setSuiteThreadPoolSize(cla.suiteThreadPoolSize);
		setRandomizeSuites(cla.randomizeSuites);
	}

	public void setSuiteThreadPoolSize(Integer suiteThreadPoolSize) {
		m_suiteThreadPoolSize = suiteThreadPoolSize;
	}

	public Integer getSuiteThreadPoolSize() {
		return m_suiteThreadPoolSize;
	}

	public void setRandomizeSuites(boolean randomizeSuites) {
		m_randomizeSuites = randomizeSuites;
	}

	/**
	 * This method is invoked by Maven's Surefire, only remove it once Surefire
	 * has been modified to no longer call it.
	 */
	public void setSourcePath(String path) {
		// nop
	}

	/**
	 * This method is invoked by Maven's Surefire to configure the runner, do
	 * not remove unless you know for sure that Surefire has been updated to use
	 * the new configure(CommandLineArgs) method.
	 * 
	 * @deprecated use new configure(CommandLineArgs) method
	 */ 
	@Deprecated
	public void configure(Map cmdLineArgs) {
		CommandLineArgs result = new CommandLineArgs();

		Integer verbose = (Integer) cmdLineArgs.get(CommandLineArgs.LOG);
		if (null != verbose) {
			result.verbose = verbose;
		}
		result.outputDirectory = (String) cmdLineArgs
				.get(CommandLineArgs.OUTPUT_DIRECTORY);

		String testClasses = (String) cmdLineArgs
				.get(CommandLineArgs.TEST_CLASS);
		if (null != testClasses) {
			result.testClass = testClasses;
		}

		String testNames = (String) cmdLineArgs.get(CommandLineArgs.TEST_NAMES);
		if (testNames != null) {
			result.testNames = testNames;
		}

		String useDefaultListeners = (String) cmdLineArgs
				.get(CommandLineArgs.USE_DEFAULT_LISTENERS);
		if (null != useDefaultListeners) {
			result.useDefaultListeners = useDefaultListeners;
		}

		result.groups = (String) cmdLineArgs.get(CommandLineArgs.GROUPS);
		result.excludedGroups = (String) cmdLineArgs
				.get(CommandLineArgs.EXCLUDED_GROUPS);
		result.testJar = (String) cmdLineArgs.get(CommandLineArgs.TEST_JAR);
		result.xmlPathInJar = (String) cmdLineArgs
				.get(CommandLineArgs.XML_PATH_IN_JAR);
		result.junit = (Boolean) cmdLineArgs.get(CommandLineArgs.JUNIT);
		result.mixed = (Boolean) cmdLineArgs.get(CommandLineArgs.MIXED);
		result.master = (String) cmdLineArgs.get(CommandLineArgs.MASTER);
		result.slave = (String) cmdLineArgs.get(CommandLineArgs.SLAVE);
		result.skipFailedInvocationCounts = (Boolean) cmdLineArgs
				.get(CommandLineArgs.SKIP_FAILED_INVOCATION_COUNTS);
		String parallelMode = (String) cmdLineArgs
				.get(CommandLineArgs.PARALLEL);
		if (parallelMode != null) {
			result.parallelMode = parallelMode;
		}

		String threadCount = (String) cmdLineArgs
				.get(CommandLineArgs.THREAD_COUNT);
		if (threadCount != null) {
			result.threadCount = Integer.parseInt(threadCount);
		}

		// Not supported by Surefire yet
		Integer dptc = (Integer) cmdLineArgs
				.get(CommandLineArgs.DATA_PROVIDER_THREAD_COUNT);
		if (dptc != null) {
			result.dataProviderThreadCount = dptc;
		}
		String defaultSuiteName = (String) cmdLineArgs
				.get(CommandLineArgs.SUITE_NAME);
		if (defaultSuiteName != null) {
			result.suiteName = defaultSuiteName;
		}

		String defaultTestName = (String) cmdLineArgs
				.get(CommandLineArgs.TEST_NAME);
		if (defaultTestName != null) {
			result.testName = defaultTestName;
		}

		Object listeners = cmdLineArgs.get(CommandLineArgs.LISTENER);
		if (listeners instanceof List) {
			result.listener = Utils.join((List<?>) listeners, ",");
		} else {
			result.listener = (String) listeners;
		}

		String ms = (String) cmdLineArgs.get(CommandLineArgs.METHOD_SELECTORS);
		if (null != ms) {
			result.methodSelectors = ms;
		}

		String objectFactory = (String) cmdLineArgs
				.get(CommandLineArgs.OBJECT_FACTORY);
		if (null != objectFactory) {
			result.objectFactory = objectFactory;
		}

		String runnerFactory = (String) cmdLineArgs
				.get(CommandLineArgs.TEST_RUNNER_FACTORY);
		if (null != runnerFactory) {
			result.testRunnerFactory = runnerFactory;
		}

		String reporterConfigs = (String) cmdLineArgs
				.get(CommandLineArgs.REPORTER);
		if (reporterConfigs != null) {
			result.reporter = reporterConfigs;
		}

		String failurePolicy = (String) cmdLineArgs
				.get(CommandLineArgs.CONFIG_FAILURE_POLICY);
		if (failurePolicy != null) {
			result.configFailurePolicy = failurePolicy;
		}

		configure(result);
	}

	/**
	 * Only run the specified tests from the suite.
	 */
	public void setTestNames(List<String> testNames) {
		m_testNames = testNames;
	}

	public void setSkipFailedInvocationCounts(Boolean skip) {
		m_skipFailedInvocationCounts = skip;
	}

	private void addReporter(ReporterConfig reporterConfig) {
		Object instance = reporterConfig.newReporterInstance();
		if (instance != null) {
			addListener(instance);
		} else {
			LOGGER.warn("Could not find reporte class : "
					+ reporterConfig.getClassName());
		}
	}

	/**
	 * Specify if this run should be in Master-Slave mode as Master
	 * 
	 * @param fileName
	 *            remote.properties path
	 */
	public void setMaster(String fileName) {
		m_masterfileName = fileName;
	}

	/**
	 * Specify if this run should be in Master-Slave mode as slave
	 * 
	 * @param fileName
	 *            remote.properties path
	 */
	public void setSlave(String fileName) {
		m_slavefileName = fileName;
	}

	/**
	 * Specify if this run should be made in JUnit mode
	 * 
	 * @param isJUnit
	 */
	public void setJUnit(Boolean isJUnit) {
		m_isJUnit = isJUnit;
	}

	/**
	 * Specify if this run should be made in mixed mode
	 */
	public void setMixed(Boolean isMixed) {
		if (isMixed == null) {
			return;
		}
		m_isMixed = isMixed;
	}

	/**
	 * @deprecated The TestNG version is now established at load time. This
	 *             method is not required anymore and is now a no-op.
	 */
	@Deprecated
	public static void setTestNGVersion() {
		LOGGER.info("setTestNGVersion has been deprecated.");
	}

	/**
	 * Returns true if this is the JDK 1.4 JAR version of TestNG, false
	 * otherwise.
	 * 
	 * @return true if this is the JDK 1.4 JAR version of TestNG, false
	 *         otherwise.
	 */
	@Deprecated
	public static boolean isJdk14() {
		return false;
	}

	/**
	 * 检查命令行参数的传入是否有效
	 * 
	 * Double check that the command line parameters are valid.
	 */
	protected static void validateCommandLineParameters(CommandLineArgs args) {
		String testClasses = args.testClass;
		List<String> testNgXml = args.suiteFiles;
		String testJar = args.testJar;
		String slave = args.slave;
		List<String> methods = args.commandLineMethods;

		if (testClasses == null && slave == null && testJar == null
				&& (testNgXml == null || testNgXml.isEmpty())
				&& (methods == null || methods.isEmpty())) {
			throw new ParameterException(
					"You need to specify at least one testng.xml, one class"
							+ " or one method");
		}

		String groups = args.groups;
		String excludedGroups = args.excludedGroups;

		if (testJar == null && (null != groups || null != excludedGroups)
				&& testClasses == null
				&& (testNgXml == null || testNgXml.isEmpty())) {
			throw new ParameterException(
					"Groups option should be used with testclass option");
		}

		if (args.slave != null && args.master != null) {
			throw new ParameterException(CommandLineArgs.SLAVE
					+ " can't be combined with " + CommandLineArgs.MASTER);
		}

		Boolean junit = args.junit;
		Boolean mixed = args.mixed;
		if (junit && mixed) {
			throw new ParameterException(CommandLineArgs.MIXED
					+ " can't be combined with " + CommandLineArgs.JUNIT);
		}
	}

	/**
	 * @return true if at least one test failed.
	 */
	public boolean hasFailure() {
		return (getStatus() & HAS_FAILURE) == HAS_FAILURE;
	}

	/**
	 * @return true if at least one test failed within success percentage.
	 */
	public boolean hasFailureWithinSuccessPercentage() {
		return (getStatus() & HAS_FSP) == HAS_FSP;
	}

	/**
	 * @return true if at least one test was skipped.
	 */
	public boolean hasSkip() {
		return (getStatus() & HAS_SKIPPED) == HAS_SKIPPED;
	}

	static void exitWithError(String msg) {
		System.err.println(msg);
		usage();
		System.exit(1);
	}

	public String getOutputDirectory() {
		return m_outputDir;
	}

	// 获取当前的注释转换器
	public IAnnotationTransformer getAnnotationTransformer() {
		return m_annotationTransformer;
	}

	public void setAnnotationTransformer(IAnnotationTransformer t) {
		// compare by reference!
		if (m_annotationTransformer != m_defaultAnnoProcessor
				&& m_annotationTransformer != t) {
			LOGGER.warn("AnnotationTransformer already set");
		}
		m_annotationTransformer = t;
	}

	/**
	 * @return the defaultSuiteName
	 */
	public String getDefaultSuiteName() {
		return m_defaultSuiteName;
	}

	/**
	 * @param defaultSuiteName
	 *            the defaultSuiteName to set
	 */
	public void setDefaultSuiteName(String defaultSuiteName) {
		m_defaultSuiteName = defaultSuiteName;
	}

	/**
	 * @return the defaultTestName
	 */
	public String getDefaultTestName() {
		return m_defaultTestName;
	}

	/**
	 * @param defaultTestName
	 *            the defaultTestName to set
	 */
	public void setDefaultTestName(String defaultTestName) {
		m_defaultTestName = defaultTestName;
	}

	/**
	 * Sets the policy for whether or not to ever invoke a configuration method
	 * again after it has failed once. Possible values are defined in
	 * {@link XmlSuite}. The default value is {@link XmlSuite#SKIP}.
	 * 
	 * @param failurePolicy
	 *            the configuration failure policy
	 */
	public void setConfigFailurePolicy(String failurePolicy) {
		m_configFailurePolicy = failurePolicy;
	}

	/**
	 * Returns the configuration failure policy.
	 * 
	 * @return config failure policy
	 */
	public String getConfigFailurePolicy() {
		return m_configFailurePolicy;
	}

	// DEPRECATED: to be removed after a major version change
	/**
	 * @deprecated since 5.1
	 */
	@Deprecated
	public static TestNG getDefault() {
		return m_instance;
	}

	/**
	 * @deprecated since 5.1
	 */
	@Deprecated
	public void setHasFailure(boolean hasFailure) {
		m_status |= HAS_FAILURE;
	}

	/**
	 * @deprecated since 5.1
	 */
	@Deprecated
	public void setHasFailureWithinSuccessPercentage(
			boolean hasFailureWithinSuccessPercentage) {
		m_status |= HAS_FSP;
	}

	/**
	 * @deprecated since 5.1
	 */
	@Deprecated
	public void setHasSkip(boolean hasSkip) {
		m_status |= HAS_SKIPPED;
	}

	/**
	 * 字面意思貌似是强行退出时候的监听器，做对应的处理 TODO
	 * 
	 * @date 2014-5-14 上午10:36:23
	 *
	 */
	public static class ExitCodeListener implements IResultListener2 {
		private TestNG m_mainRunner;

		public ExitCodeListener() {
			m_mainRunner = TestNG.m_instance;
		}
		
		/**
		 * 构造器传入TestNG启动对象，可能需要在特定场景对核心
		 * 启动对象做操作
		 * 
		 * @param runner
		 */
		public ExitCodeListener(TestNG runner) {
			m_mainRunner = runner;
		}

		@Override
		public void beforeConfiguration(ITestResult tr) {
		}

		@Override
		public void onTestFailure(ITestResult result) {
			// 都失败了为什么还要设置测试用例在跑? TODO
			setHasRunTests();
			
			// 传递一个测试失败的状态
			m_mainRunner.setStatus(HAS_FAILURE);
		}

		@Override
		public void onTestSkipped(ITestResult result) {
			// 设置测试用例状态在跑中
			setHasRunTests();
			
			// 传递一个测试跳过的状态
			m_mainRunner.setStatus(HAS_SKIPPED);
		}

		/**
		 * 这个方法的含义没太懂 TODO
		 */
		@Override
		public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
			// 设置测试用例状态在跑中
			setHasRunTests();
			
			// 传递一个FSP的状态
			m_mainRunner.setStatus(HAS_FSP);
		}

		@Override
		public void onTestSuccess(ITestResult result) {
			// 设置测试用例状态在跑中
			setHasRunTests();
		}

		@Override
		public void onStart(ITestContext context) {
			// 设置测试用例状态在跑中
			setHasRunTests();
		}

		@Override
		public void onFinish(ITestContext context) {
		}
		
		@Override
		public void onTestStart(ITestResult result) {
			// 设置测试用例状态在跑中
			setHasRunTests();
		}

		/**
		 * 设定TestNG的hasTest状态为true
	     * 应该是代表当前有测试用例在跑
		 */
		private void setHasRunTests() {
			m_mainRunner.m_hasTests = true;
		}

		/**
		 * @see org.testng.IConfigurationListener#onConfigurationFailure(org.testng.ITestResult)
		 */
		@Override
		public void onConfigurationFailure(ITestResult itr) {
			// 配置出错，设置TestNG状态出错
			m_mainRunner.setStatus(HAS_FAILURE);
		}

		/**
		 * @see org.testng.IConfigurationListener#onConfigurationSkip(org.testng.ITestResult)
		 */
		@Override
		public void onConfigurationSkip(ITestResult itr) {
			// 配置跳过，设置TestNG状态跳过
			m_mainRunner.setStatus(HAS_SKIPPED);
		}

		/**
		 * @see org.testng.IConfigurationListener#onConfigurationSuccess(org.testng.ITestResult)
		 */
		@Override
		public void onConfigurationSuccess(ITestResult itr) {
		}
	}

	private void setConfigurable(IConfigurable c) {
		// compare by reference!
		if (m_configurable != null && m_configurable != c) {
			LOGGER.warn("Configurable already set");
		}
		m_configurable = c;
	}

	private void setHookable(IHookable h) {
		// compare by reference!
		if (m_hookable != null && m_hookable != h) {
			LOGGER.warn("Hookable already set");
		}
		m_hookable = h;
	}

	public void setMethodInterceptor(IMethodInterceptor i) {
		// compare by reference!
		if (m_methodInterceptor != null && m_methodInterceptor != i) {
			LOGGER.warn("MethodInterceptor already set");
		}
		m_methodInterceptor = i;
	}

	public void setDataProviderThreadCount(int count) {
		m_dataProviderThreadCount = count;
	}

	/** Add a class loader to the searchable loaders. */
	public void addClassLoader(final ClassLoader loader) {
		if (loader != null) {
			ClassHelper.addClassLoader(loader);
		}
	}

	public void setPreserveOrder(boolean b) {
		m_preserveOrder = b;
	}

	protected long getStart() {
		return m_start;
	}

	protected long getEnd() {
		return m_end;
	}

	public void setGroupByInstances(boolean b) {
		m_groupByInstances = b;
	}

	// ///
	// ServiceLoader testing
	//

	private URLClassLoader m_serviceLoaderClassLoader;
	private List<ITestNGListener> m_serviceLoaderListeners = Lists
			.newArrayList();

	/*
	 * Used to test ServiceClassLoader
	 */
	public void setServiceLoaderClassLoader(URLClassLoader ucl) {
		m_serviceLoaderClassLoader = ucl;
	}

	/*
	 * Used to test ServiceClassLoader
	 */
	private void addServiceLoaderListener(ITestNGListener l) {
		m_serviceLoaderListeners.add(l);
	}

	/*
	 * Used to test ServiceClassLoader
	 */
	public List<ITestNGListener> getServiceLoaderListeners() {
		return m_serviceLoaderListeners;
	}

	//
	// ServiceLoader testing
	// ///
}
