package com.asiainfo.easymem;

import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.util.Resource;

/**
 * ��ȡeasymem.configure�������ļ�
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
				log.info("û�з���-Deasymem.configure��������,���Ի�ȡĬ�ϵ�easymem.properties");
				prop = Resource.loadPropertiesFromClassPath("easymem.properties");
			} else {
				prop = Resource.loadPropertiesFromClassPath(config);
			}
		} catch (Exception ex) {
			throw new RuntimeException("��ʼ��ʧ��", ex);
		}
	}

	private EasyMemConfigure() {
	}

	/** ��û������� */
	public static Properties getProperties() {
		return prop;
	}

	/** ���������ļ� */
	public static void setProperties(Properties newProperties) {
		prop.clear();
		Set keys = newProperties.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Object item = (Object) iter.next();
			prop.put(item, newProperties.get(item));
		}
	}

	/** ���������ļ�ת���������� */
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
