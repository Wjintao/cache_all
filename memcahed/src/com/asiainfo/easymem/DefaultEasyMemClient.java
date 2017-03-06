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
 * 默认的esaymem的客户端
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
						log.info("找到server.driver=[" + driverClassName + "],采用此驱动");
					}
					DRIVER_CLASS_NAME = driverClassName;
					driver = (IEasyMemDriver) obj;
				}
			} else {
				if (log.isInfoEnabled()) {
					log.info("没有找到server.driver,采用默认的driver[" + EasyMemBufferedDriver.class.getName() + "]");
				}
				DRIVER_CLASS_NAME = EasyMemBufferedDriver.class.getName();
				driver = new EasyMemBufferedDriver();
			}
		} catch (Exception ex) {
			log.error("获得driver失败,采用默认的driver[" + EasyMemBufferedDriver.class.getName() + "],不影响系统的运行", ex);
		}
	}

	private DefaultEasyMemClient() {
	}

	public static DefaultEasyMemClient getInstance() throws Exception {
		if (instance == null) {
			synchronized (isInit) {
				if (isInit.equals(Boolean.FALSE)) {
					// 启动定时任务，每隔一段时间验证一下失败的pool，验证成功重新连接pool
					Timer timer = new Timer(true);

					int delay = 0;
					int period = 0;
					try {
						//连接池检查任务						
						String strDelay = EasyMemConfigure.getProperties("server.checktask", true).getProperty("delay");		//启动后延迟开始,单位为秒
						String strPeriod = EasyMemConfigure.getProperties("server.checktask", true).getProperty("period");		//间隔时间,单位为秒
						delay = Integer.parseInt(strDelay) * 1000;
						period = Integer.parseInt(strPeriod) * 1000;
					} catch (Exception ex) {
						log.error("格式出错,采用默认设置", ex);
						delay = 5 * 1000;
						period = 30 * 1000;
					}

					timer.schedule(new CheckTask(), delay, period);

					String strRetry = EasyMemConfigure.getProperties().getProperty("server.failover.retry");		//容灾的重试次数,方式为立即重试
					try {
						retry = Integer.parseInt(strRetry);
					} catch (Throwable ex) {
						log.error("格式出错,采用默认设置", ex);
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

	/** 批量获得 */
	public Map getMultiArray(String[] keys) throws Exception {
		Map rtn = null;
		try {
			rtn = driver.get(keys);
		} catch (Exception ex) {
			int i = 0;
			for (; i < retry; i++) {
				try {
					rtn = driver.get(keys);
					log.error("第" + (i + 1) + "次重试成功", ex);
				} catch (Exception ex2) {
					log.error("第" + (i + 1) + "次重试失败", ex2);
					if ((i + 1) == retry) {
						log.error("到达重试最大次数,抛出异常");
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
					log.error("第" + (i + 1) + "次重试成功");
				} catch (Exception ex2) {
					log.error("第" + (i + 1) + "次重试失败", ex2);
					if ((i + 1) == retry) {
						log.error("到达重试最大次数,抛出异常");
						throw ex;
					}
					continue;
				}
				break;
			}
		}
		return rtn;
	}

	/** 删除key */
	public boolean delete(String key) throws Exception {
		return driver.delete(key);
	}

}
