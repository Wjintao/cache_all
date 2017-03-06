package com.asiainfo.easymem;

import java.util.HashMap;
import java.util.Timer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import org.apache.commons.lang.StringUtils;

import com.asiainfo.easymem.driver.IEasyMemDriver;
import com.asiainfo.easymem.driver.EasyMemBufferedDriver;

/**
 * Ĭ�ϵ�esaymem�Ŀͻ���
 * 
 * @author linzhaoming
 * 
 * Created at 2012-10-15
 */
public class DefaultEasyMemClient {
	private transient static Log log = LogFactory.getLog(DefaultEasyMemClient.class);

	public static String DRIVER_CLASS_NAME = null;

	private static DefaultEasyMemClient instance = null;
	private static IEasyMemDriver driver = null;

	private static int retry = 0;
	private static Boolean isInit = Boolean.FALSE;

	static {
		try {
			String driverClassName = EasyMemConfigure.getProperties().getProperty("server.driver");
			if (!StringUtils.isBlank(driverClassName)) {
				Object obj = Class.forName(driverClassName).newInstance();
				if (obj instanceof IEasyMemDriver) {
					if (log.isInfoEnabled()) {
						log.info("�ҵ�server.driver=[" + driverClassName + "],���ô�����");
					}
					DRIVER_CLASS_NAME = driverClassName;
					driver = (IEasyMemDriver) obj;
				}
			} else {
				if (log.isInfoEnabled()) {
					log.info("û���ҵ�server.driver,����Ĭ�ϵ�driver[" + EasyMemBufferedDriver.class.getName() + "]");
				}
				DRIVER_CLASS_NAME = EasyMemBufferedDriver.class.getName();
				driver = new EasyMemBufferedDriver();
			}
		} catch (Exception ex) {
			log.error("���driverʧ��,����Ĭ�ϵ�driver[" + EasyMemBufferedDriver.class.getName() + "],��Ӱ��ϵͳ������", ex);
		}
	}

	private DefaultEasyMemClient() {
	}

	public static DefaultEasyMemClient getInstance() throws Exception {
		if (instance == null) {
			synchronized (isInit) {
				if (isInit.equals(Boolean.FALSE)) {
					// ������ʱ����ÿ��һ��ʱ����֤һ��ʧ�ܵ�pool����֤�ɹ���������pool
					Timer timer = new Timer(true);

					int delay = 0;
					int period = 0;
					try {
						//���ӳؼ������						
						String strDelay = EasyMemConfigure.getProperties("server.checktask", true).getProperty("delay");		//�������ӳٿ�ʼ,��λΪ��
						String strPeriod = EasyMemConfigure.getProperties("server.checktask", true).getProperty("period");		//���ʱ��,��λΪ��
						delay = Integer.parseInt(strDelay) * 1000;
						period = Integer.parseInt(strPeriod) * 1000;
					} catch (Exception ex) {
						log.error("��ʽ����,����Ĭ������", ex);
						delay = 5 * 1000;
						period = 30 * 1000;
					}

					timer.schedule(new CheckTask(), delay, period);

					String strRetry = EasyMemConfigure.getProperties().getProperty("server.failover.retry");		//���ֵ����Դ���,��ʽΪ��������
					try {
						retry = Integer.parseInt(strRetry);
					} catch (Throwable ex) {
						log.error("��ʽ����,����Ĭ������", ex);
						retry = 0;
					}

					instance = new DefaultEasyMemClient();
					isInit = Boolean.TRUE;
				}
			}
		}
		return instance;
	}

	public HashMap getStat() throws Exception {
		return driver.stats();
	}

	/** ������� */
	public Map getMultiArray(String[] keys) throws Exception {
		Map rtn = null;
		try {
			rtn = driver.get(keys);
		} catch (Exception ex) {
			int i = 0;
			for (; i < retry; i++) {
				try {
					rtn = driver.get(keys);
					log.error("��" + (i + 1) + "�����Գɹ�", ex);
				} catch (Exception ex2) {
					log.error("��" + (i + 1) + "������ʧ��", ex2);
					if ((i + 1) == retry) {
						log.error("��������������,�׳��쳣");
						throw ex;
					}
					continue;
				}
				break;
			}
		}
		return rtn;
	}

	public boolean setKeyAndValue2AllServer(String key, Object obj) throws Exception {
		return driver.setKeyAndValue2AllServer(key, obj);
	}

	public Object get(String key) throws Exception {
		Object rtn = null;
		try {
			rtn = driver.get(key);
		} catch (Exception ex) {
			int i = 0;
			for (; i < retry; i++) {
				try {
					rtn = driver.get(key);
					log.error("��" + (i + 1) + "�����Գɹ�");
				} catch (Exception ex2) {
					log.error("��" + (i + 1) + "������ʧ��", ex2);
					if ((i + 1) == retry) {
						log.error("��������������,�׳��쳣");
						throw ex;
					}
					continue;
				}
				break;
			}
		}
		return rtn;
	}

	/** ɾ��key */
	public boolean delete(String key) throws Exception {
		return driver.delete(key);
	}

}
