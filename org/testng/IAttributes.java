package org.testng;

import java.io.Serializable;
import java.util.Set;

/**
 * 属性接口。很多开源框架都有类似的功能。
 * 
 * A trait that is used by all interfaces that lets the user add or remove their
 * own attributes.
 */
public interface IAttributes extends Serializable {
  /**
   * @param name The name of the attribute to return
   */
  public Object getAttribute(String name);

  /**
   * Set a custom attribute.
   */
  public void setAttribute(String name, Object value);

  /**
   * @return all the attributes names.
   */
  public Set<String> getAttributeNames();

  /**
   * Remove the attribute
   *
   * @return the attribute value if found, null otherwise
   */
  public Object removeAttribute(String name);
}
