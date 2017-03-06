package com.asiainfo.easymem.driver.io;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.EasyMemConfigure;

/**
 * IO工厂
 * 
 * @author linzhaoming
 * 
 * Created at 2012-10-15
 */
public final class IOFactory {
	private transient static Log log = LogFactory.getLog(IOFactory.class);

	private static Boolean isInit = Boolean.FALSE;
	private static ISerializable SERIALIZABLE = null;

	static {
		init();
	}

	private IOFactory() {
	}

	/** 初始化 */
	public static void init() {
		if (isInit.equals(Boolean.FALSE)) {
			synchronized (isInit) {
				if (isInit.equals(Boolean.FALSE)) {
					try {
						double javaClassVersion = Double.parseDouble(System.getProperty("java.class.version"));
						String io = EasyMemConfigure.getProperties().getProperty("server.io.serialization");
						if (!StringUtils.isBlank(io)) {
							if (io.trim().equalsIgnoreCase("java")) {
								// 采用java的序列化
								SERIALIZABLE = new JavaSerializable();
							} else if (io.trim().equalsIgnoreCase("hessian")) {
								// 采用hessian的序列化
								if (javaClassVersion > 48) {
									// jdk1.5+
									SERIALIZABLE = new HessianSerializable();
								} else {
									if (log.isInfoEnabled()) {
										log.info("server.io配置采用hessian序列化方式,但是目前jdk版本小于1.5,hessian必须采用jdk1.5(+)版本,所以目前改变为标准的java序列化模式");
									}
									// jdk1.4-
									SERIALIZABLE = new JavaSerializable();
								}
							} else {
								// 默认采用java的序列化
								SERIALIZABLE = new JavaSerializable();
							}
						} else {
							// 默认采用hessian的序列化
							SERIALIZABLE = new JavaSerializable();
						}
					} catch (Throwable ex) {
						log.error("获取序列化方式失败,不影响正常运行,采用标准的java序列化方式", ex);
						SERIALIZABLE = new JavaSerializable();
					} finally {
						isInit = Boolean.TRUE;
						log.info("序列化方式采用" + SERIALIZABLE.getClass().getName());
					}
				}
			}
		}
	}

	/** 对象转成字节数组 */
	public static byte[] object2bytes(Object object) throws IOException {
		return SERIALIZABLE.object2bytes(object);
	}

	/** 字节数组转成对象 */
	public static Object bytes2object(byte[] bytes) throws IOException, ClassNotFoundException {
		return SERIALIZABLE.bytes2object(bytes);
	}

}
