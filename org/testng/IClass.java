package org.testng;

import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;

import java.io.Serializable;

/**
 * IClass代表了一个测试类以及一组他的实例集合。
 * 
 * <code>IClass</code> represents a test class and a collection of its
 * instances.
 * 
 * @author <a href = "mailto:cedric&#64;beust.com">Cedric Beust</a>
 */
@SuppressWarnings("rawtypes")
public interface IClass extends Serializable {

	/**
	 * 当前测试类的类名
	 * 
	 * @return this test class name. This is the name of the corresponding Java class.
	 */
	String getName();

	/**
	 *  返回test节点的XML表示。
	 *  
	 *  <suite name="Default suite">
	 *	  <test verbose="2" name="Default test">    ---------XmlTest负责的部分开始
	 *	    <classes>
	 *	      <class name="com.alibaba.ceres.util.DateUtilTest"/>
	 *	    </classes>
	 *	  </test>  									---------XmlTest负责的部分结束
	 *	</suite>  
	 *
	 * @return the &lt;test&gt; tag this class was found in.
	 */
	XmlTest getXmlTest();

	/**
	 *  返回class节点的XML表示。
	 *  
	 *  <suite name="Default suite">
	 *	  <test verbose="2" name="Default test">    
	 *	    <classes>								---------XmlClass负责的部分开始
	 *	      <class name="com.alibaba.ceres.util.DateUtilTest"/>
	 *	    </classes>								---------XmlClass负责的部分结束
	 *	  </test>  									
	 *	</suite>  
	 *
	 * @return the *lt;class&gt; tag this class was found in.
	 */
	XmlClass getXmlClass();

	/**
	 * 如果一个单元测试实现了ITest接口，返回对应的testName(ITest有定义这样一个方法)
	 * 否则返回null。
	 * 
	 * If this class implements ITest, returns its test name, otherwise returns
	 * null.
	 */
	String getTestName();

	/**
	 * 返回真正的class。
	 * 
	 * @return the Java class corresponding to this IClass.
	 */
	Class getRealClass();

	/**
	 * need ensure  TODO
	 * 
	 * @param create
	 * @return
	 */
	Object[] getInstances(boolean create);

	/**
	 * 得到实例数目。
	 * 
	 * @return
	 */
	int getInstanceCount();

	/**
	 * need ensure TODO
	 * 
	 * @return
	 */
	long[] getInstanceHashCodes();

	/**
	 * 添加一个实例
	 * 
	 * @param instance
	 */
	void addInstance(Object instance);
}






