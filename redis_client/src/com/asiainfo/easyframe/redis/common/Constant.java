package com.asiainfo.easyframe.redis.common;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Constant {
	private static transient Log log = LogFactory.getLog(Constant.class);
	private static Properties properties = null;

	static {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("redisClient.properties");
		properties = new Properties();
		try {
			properties.load(in);
		} catch (Exception e) {
			log.error("加载redisClient.properties配置文件出错！", e);
		}
	}

	public static String getProperty(String propertyKey) {
		return properties.getProperty(propertyKey);
	}
}
