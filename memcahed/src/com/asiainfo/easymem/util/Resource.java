package com.asiainfo.easymem.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.io.InputStream;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * ��Դ����
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class Resource {

	/**
	 * ����ļ������classpathĿ¼�� ����:config/com/ai/Test.bo ��ôֻ��¼��com/ai/Test.bo
	 * ���Դ�jar��װ���ļ���
	 */
	public static String loadFileFromClassPath(String filePath) throws Exception {
		java.io.InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String tmp = null;
		StringBuffer sb = new StringBuffer();
		while (true) {
			tmp = br.readLine();
			if (tmp != null) {
				sb.append(tmp);
				sb.append("\n");
			} else {
				break;
			}
		}
		return sb.toString();
	}

	/** ��classpath����jar�л���ļ���URL */
	public static URL loadURLFromClassPath(String filePath) throws Exception {
		return Thread.currentThread().getContextClassLoader().getResource(filePath);
	}

	/** ��classpath����jar�л���ļ��������� */
	public static InputStream loadInputStreamFromClassPath(String filePath) throws Exception {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
	}

	/** ���������ļ�ת���������� */
	public static PropertiesConfiguration loadPropertiesConfigurationFromClassPath(String filePath) throws Exception {
		PropertiesConfiguration pc = new PropertiesConfiguration();
		pc.load(Resource.loadInputStreamFromClassPath(filePath));
		return pc;
	}

	/** ���������ļ�ת���������� */
	public static Properties loadPropertiesFromClassPath(String filePath) throws Exception {
		Properties pc = new Properties();
		pc.load(Resource.loadInputStreamFromClassPath(filePath));
		return pc;
	}

	/** ���������ļ�ת���������� */
	public static Properties loadPropertiesFromClassPath(String filePath, String prefix, boolean isDiscardPrefix) throws Exception {
		Properties rtn = new Properties();
		Properties pc = loadPropertiesFromClassPath(filePath);
		Set key = pc.keySet();
		for (Iterator iter = key.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (StringUtils.indexOf(element, prefix) != -1) {
				if (isDiscardPrefix == true) {
					rtn.put(StringUtils.replace(element, prefix + ".", "").trim(), pc.get(element));
				} else {
					rtn.put(element, pc.get(element));
				}
			}
		}
		return rtn;
	}

	public static void main(String[] args) throws Exception {
		InputStream is = Resource.loadInputStreamFromClassPath("config.properties");
		PropertiesConfiguration pc = new PropertiesConfiguration();
		pc.load(is);

	}
}
