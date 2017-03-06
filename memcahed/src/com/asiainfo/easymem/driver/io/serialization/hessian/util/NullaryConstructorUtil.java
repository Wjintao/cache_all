package com.asiainfo.easymem.driver.io.serialization.hessian.util;

import java.util.HashMap;
import sun.reflect.ReflectionFactory;
import java.lang.reflect.Constructor;


/**
 * 空构造函数的工具类
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2007</p>
 * <p>Company: AI(NanJing)</p>
 *
 * @author Yang Hua
 * @version 2.0
 */
public final class NullaryConstructorUtil {
  private static final HashMap CACHE = new HashMap();
  private static ReflectionFactory REL_FAC = ReflectionFactory.getReflectionFactory();

  private NullaryConstructorUtil() {
  }

  /**
   *
   * @param clazz Class
   * @return Constructor
   */
  public static Constructor getNullaryConstructor(Class clazz) {
    Constructor rtn = null;

    if (CACHE.containsKey(clazz)) {
      rtn = (Constructor) CACHE.get(clazz);
    }
    else {
      synchronized (CACHE) {
	if (!CACHE.containsKey(clazz)) {
	  try {
	    rtn = REL_FAC.newConstructorForSerialization(clazz, Object.class.getConstructor());
	  }
	  catch (Exception ex) {
	    throw new RuntimeException(ex);
	  }
	  CACHE.put(clazz, rtn);
	}
	else{
	  rtn = (Constructor) CACHE.get(clazz);
	}
      }
    }

    return rtn;
  }
}
