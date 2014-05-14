package org.testng;

import org.testng.xml.XmlSuite;

import java.util.List;

/**
 * 报表接口。前2个参数貌似不好指定。TODO
 * 
 * This interface can be implemented by clients to generate a report.  Its method
 * generateReport() will be invoked after all the suite have run and the parameters
 * give all the test results that happened during that run.
 *
 * @author cbeust
 * Feb 17, 2006
 */
public interface IReporter extends ITestNGListener {
  /**
   * 根据指定的suites和目录生成报表。
   * 
   * Generate a report for the given suites into the specified output directory.
   */
  void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory);
}
