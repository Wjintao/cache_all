package com.asiainfo.easymem.driver.io;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.EasyMemConfigure;

/**
 * IO����
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

	/** ��ʼ�� */
	public static void init() {
		if (isInit.equals(Boolean.FALSE)) {
			synchronized (isInit) {
				if (isInit.equals(Boolean.FALSE)) {
					try {
						double javaClassVersion = Double.parseDouble(System.getProperty("java.class.version"));
						String io = EasyMemConfigure.getProperties().getProperty("server.io.serialization");
						if (!StringUtils.isBlank(io)) {
							if (io.trim().equalsIgnoreCase("java")) {
								// ����java�����л�
								SERIALIZABLE = new JavaSerializable();
							} else if (io.trim().equalsIgnoreCase("hessian")) {
								// ����hessian�����л�
								if (javaClassVersion > 48) {
									// jdk1.5+
									SERIALIZABLE = new HessianSerializable();
								} else {
									if (log.isInfoEnabled()) {
										log.info("server.io���ò���hessian���л���ʽ,����Ŀǰjdk�汾С��1.5,hessian�������jdk1.5(+)�汾,����Ŀǰ�ı�Ϊ��׼��java���л�ģʽ");
									}
									// jdk1.4-
									SERIALIZABLE = new JavaSerializable();
								}
							} else {
								// Ĭ�ϲ���java�����л�
								SERIALIZABLE = new JavaSerializable();
							}
						} else {
							// Ĭ�ϲ���hessian�����л�
							SERIALIZABLE = new JavaSerializable();
						}
					} catch (Throwable ex) {
						log.error("��ȡ���л���ʽʧ��,��Ӱ����������,���ñ�׼��java���л���ʽ", ex);
						SERIALIZABLE = new JavaSerializable();
					} finally {
						isInit = Boolean.TRUE;
						log.info("���л���ʽ����" + SERIALIZABLE.getClass().getName());
					}
				}
			}
		}
	}

	/** ����ת���ֽ����� */
	public static byte[] object2bytes(Object object) throws IOException {
		return SERIALIZABLE.object2bytes(object);
	}

	/** �ֽ�����ת�ɶ��� */
	public static Object bytes2object(byte[] bytes) throws IOException, ClassNotFoundException {
		return SERIALIZABLE.bytes2object(bytes);
	}

}
