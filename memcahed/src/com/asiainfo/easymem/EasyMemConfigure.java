package com.asiainfo.easymem;

import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.util.Resource;

/**
 * 读取easymem.configure的配置文件
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public final class EasyMemConfigure {
	private transient static Log log = LogFactory.getLog(EasyMemConfigure.class);

	private static Properties prop = null;

	static {
		try {
			String config = System.getProperty("easymem.configure");
			if (StringUtils.isBlank(config)) {
				log.info("没有发现-Deasymem.configure属性配置,尝试获取默认的easymem.properties");
				prop = Resource.loadPropertiesFromClassPath("easymem.properties");
			} else {
				prop = Resource.loadPropertiesFromClassPath(config);
			}
		} catch (Exception ex) {
			throw new RuntimeException("初始化失败", ex);
		}
	}

	private EasyMemConfigure() {
	}

	/** 获得基本配置 */
	public static Properties getProperties() {
		return prop;
	}

	/** 设置属性文件 */
	public static void setProperties(Properties newProperties) {
		prop.clear();
		Set keys = newProperties.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Object item = (Object) iter.next();
			prop.put(item, newProperties.get(item));
		}
	}

	/** 根据配置文件转载配置属性 */
	public static Properties getProperties(String prefix, boolean isDiscardPrefix) throws Exception {
		Properties rtn = new Properties();
		Set key = prop.keySet();
		for (Iterator iter = key.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (StringUtils.indexOf(element, prefix) != -1) {
				if (isDiscardPrefix == true) {
					rtn.put(StringUtils.replace(element, prefix + ".", "").trim(), prop.get(element));
				} else {
					rtn.put(element, prop.get(element));
				}
			}
		}
		return rtn;
	}

}
