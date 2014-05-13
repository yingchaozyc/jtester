package org.testng.internal;

import org.testng.IConfigurable;
import org.testng.IConfigurationListener;
import org.testng.IExecutionListener;
import org.testng.IHookable;
import org.testng.ITestObjectFactory;
import org.testng.internal.annotations.IAnnotationFinder;

import java.util.List;

/**
 * 核心的配置管理接口
 * 
 * @date 2014-5-13 下午5:11:39
 *
 */
public interface IConfiguration {
  /**
   * 获取IAnnotationFinder
   *  
   * @return
   */
  IAnnotationFinder getAnnotationFinder();
  
  /**
   * 设置IAnnotationFinder
   *  
   * @return
   */
  void setAnnotationFinder(IAnnotationFinder finder);

  /**
   * 获取ITestObjectFactory
   *  
   * @return
   */
  ITestObjectFactory getObjectFactory();
  
  /**
   * 设置ITestObjectFactory
   *  
   * @return
   */
  void setObjectFactory(ITestObjectFactory m_objectFactory);

  /**
   * 获取IHookable
   *  
   * @return
   */
  IHookable getHookable();
  
  /**
   * 设置IHookable
   *  
   * @return
   */
  void setHookable(IHookable h);

  /**
   * 获取IConfigurable
   *  
   * @return
   */
  IConfigurable getConfigurable();
  
  /**
   * 设置IConfigurable
   *  
   * @return
   */
  void setConfigurable(IConfigurable c);

  /**
   * 获取 List<IExecutionListener>列表。
   *  
   * @return
   */
  List<IExecutionListener> getExecutionListeners();
  
  /**
   * 添加IExecutionListener到列表中。
   *  
   * @return
   */
  void addExecutionListener(IExecutionListener l);

  /**
   * 获取 List<IConfigurationListener> 列表。
   *  
   * @return
   */
  List<IConfigurationListener> getConfigurationListeners();
  
  /**
   * 添加IConfigurationListener到列表中。
   *  
   * @return
   */
  void addConfigurationListener(IConfigurationListener cl);
}








