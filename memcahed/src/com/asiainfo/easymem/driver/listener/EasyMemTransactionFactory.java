package com.asiainfo.easymem.driver.listener;

import org.apache.commons.lang.StringUtils;
import java.util.Map;
import java.util.HashMap;

import com.asiainfo.easymem.EasyMemConfigure;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * 
 * @author linzhaoming
 * 
 * Created at 2012-10-15
 */
public final class EasyMemTransactionFactory {
	private transient static Log log = LogFactory.getLog(EasyMemTransactionFactory.class);
	public static final Object NULL_OBJECT = new Object();

	protected static final ThreadLocal CACHE = new ThreadLocal();
	private static boolean TRANSACTION_CACHE_ENABLE = false;
	private static boolean TRANSACTION_CACHE_NULL_CACHE = false;
	private static final String JOIN_STRING = "|";

	static {
		try {
			String isEnable = EasyMemConfigure.getProperties().getProperty("server.transaction_cache_enable");
			if (!StringUtils.isBlank(isEnable)) {
				if (isEnable.trim().equalsIgnoreCase("true") || isEnable.trim().equalsIgnoreCase("1")) {
					TRANSACTION_CACHE_ENABLE = true;
				}
			}

			String isNullCache = EasyMemConfigure.getProperties().getProperty("server.transaction_cache_null_cache");
			if (!StringUtils.isBlank(isNullCache)) {
				if (isNullCache.trim().equalsIgnoreCase("true") || isNullCache.trim().equalsIgnoreCase("1")) {
					TRANSACTION_CACHE_NULL_CACHE = true;
				}
			}

		} catch (Throwable ex) {
			TRANSACTION_CACHE_ENABLE = false;
			log.error("判断参数server.enable_transaction_cache错误,本次关闭使用事务内的cache", ex);
		} finally {
			if (TRANSACTION_CACHE_ENABLE) {
				if (log.isInfoEnabled()) {
					log.info("使用事务内的cache");
				}
			} else {
				if (log.isInfoEnabled()) {
					log.info("不使用事务内的cache");
				}
			}

			if (TRANSACTION_CACHE_NULL_CACHE) {
				if (log.isInfoEnabled()) {
					log.info("事务内的cache对null进行cache");
				}
			} else {
				if (log.isInfoEnabled()) {
					log.info("事务内的cache对null不进行cache");
				}
			}
		}
	}

	private EasyMemTransactionFactory() {
	}

	/** 是否在事务内部 */
	public static boolean existTransaction() {
		if (!TRANSACTION_CACHE_ENABLE) {
			return false;
		}

		boolean rtn = false;
		if (CACHE.get() != null) {
			rtn = true;
		}
		return rtn;
	}

	/** 根据key获得对象 */
	public static Object getFromCache(String key) {
		return ((HashMap) CACHE.get()).get(key);
	}

	/** 根据key数组获得Map对象 */
	public static Map getFromCache(String[] key) {
		return (Map) ((HashMap) CACHE.get()).get(joinString(key));
	}

	/** 根据key和value设置到cache中 */
	public static void putIntoCache(String key, Object object) {
		if (TRANSACTION_CACHE_NULL_CACHE && object == null) {
			((HashMap) CACHE.get()).put(key, NULL_OBJECT);
		} else if (object != null) {
			((HashMap) CACHE.get()).put(key, object);
		}
	}

	/** 根据key和value设置到cache中 */
	public static void putIntoCache(String[] key, Map map) {
		if (TRANSACTION_CACHE_NULL_CACHE && map == null) {
			((HashMap) CACHE.get()).put(joinString(key), NULL_OBJECT);
		} else if (map != null) {
			((HashMap) CACHE.get()).put(joinString(key), map);
		}
	}

	/** 连接字符 */
	private static String joinString(String[] key) {
		return "^^" + StringUtils.join(key, JOIN_STRING);
	}

}
