package com.asiainfo.easymem.driver;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.DefaultEasyMemClient;
import com.asiainfo.easymem.EasyMemConfigure;
import com.asiainfo.easymem.policy.LoadBalanceFactory;
import com.asiainfo.easymem.pool.SocketObjectPool;

class LocalCacheTask extends TimerTask {
	private static transient Log log = LogFactory.getLog(LocalCacheTask.class);

	private long[] fixedTime = null;
	private int statDiffTime = 10;
	private String checkWarmKey = null;
	private Map checkSnapshot = new HashMap();

	public LocalCacheTask(String[] configFixedTime, int statDiffTime) {
		this.statDiffTime = statDiffTime;

		if (configFixedTime != null) {
			GregorianCalendar cal = new GregorianCalendar();
			Date date = new Date(System.currentTimeMillis());
			cal.setTime(date);
			int nowHour = cal.get(11);
			int nowMinute = cal.get(12);

			List list = new ArrayList();

			for (int i = 0; i < configFixedTime.length; i++) {
				if (!StringUtils.isBlank(configFixedTime[i])) {
					String[] tmp = StringUtils.split(configFixedTime[i], ":");
					if ((tmp == null) || (tmp.length != 2) || (StringUtils.isBlank(tmp[0])) || (!StringUtils.isNumeric(tmp[0])) || (StringUtils.isBlank(tmp[1])) || (!StringUtils.isNumeric(tmp[1]))) {
						continue;
					}
					int hour = Integer.parseInt(tmp[0]);
					int minute = Integer.parseInt(tmp[1]);
					if ((hour < 0) || (hour > 23) || (minute < 0) || (minute > 59))
						continue;
					boolean isAddDay = false;
					if (nowHour > hour) {
						isAddDay = true;
					} else if ((nowHour == hour) && (nowMinute > minute)) {
						isAddDay = true;
					}

					GregorianCalendar objGregorianCalendar = new GregorianCalendar();
					objGregorianCalendar.setTime(date);
					if (isAddDay) {
						objGregorianCalendar.add(5, 1);
					}
					objGregorianCalendar.set(11, hour);
					objGregorianCalendar.set(12, minute);
					list.add(new Long(objGregorianCalendar.getTimeInMillis()));
				}

			}

			this.fixedTime = new long[list.size()];
			int j = 0;
			for (Iterator iter = list.iterator(); iter.hasNext();) {
				Long item = (Long) iter.next();
				this.fixedTime[j] = item.longValue();
				j++;
			}

			Arrays.sort(this.fixedTime);
		}

		this.checkWarmKey = EasyMemConfigure.getProperties().getProperty("server.validate.key");
	}

	public void run() {
		Thread.currentThread().setName("easymem local cache检查线程");
		try {
			DefaultEasyMemClient.getInstance();
		} catch (Exception ex) {
			log.info("获得DefaultEasyMemClient.getInstance出错", ex);
		}

		try {
			long curTime = System.currentTimeMillis();
			if ((this.fixedTime != null) && (this.fixedTime.length > 0)) {
				for (int i = 0; i < this.fixedTime.length; i++) {
					if ((this.fixedTime[i] <= 0L) || (curTime < this.fixedTime[i]))
						continue;
					MemcachedBufferedDriverLocalCache.LOCAL_CACHE.clear();
					MemcachedBufferedDriverLocalCache.LAST_LOCAL_CACHE_CLEAR_TIME = System.currentTimeMillis();

					if (log.isInfoEnabled()) {
						log.info("清除easymem local cache");
					}

					this.fixedTime[i] += 86400000L;
					return;
				}
			}
		} catch (Throwable ex) {
			log.error("easymem local cache定时清除检查出错", ex);
		}

		Object[] ok_servers = null;
		try {
			ok_servers = LoadBalanceFactory.getInstance().getArivableServers();
			if ((ok_servers == null) && (ok_servers.length == 0)) {
				if (log.isDebugEnabled()) {
					log.info("easymem local cache定时检查,发现没有可用server");
				}
				return;
			}
		} catch (Exception ex) {
			log.info("easymem local cache定时检查,检查是否有可用的server", ex);
		}

		try {
			MemcachedBufferedDriverLocalCache driver = new MemcachedBufferedDriverLocalCache();

			if (!StringUtils.isBlank(this.checkWarmKey)) {
				List allServer = LoadBalanceFactory.getInstance().getAllServers();
				int serverCount = allServer.size();
				for (int i = 0; i < serverCount; i++) {
					try {
						driver._get(this.checkWarmKey.trim());
					} catch (Throwable ex) {
						log.info("easymem local cache定时检查stats预热出错", ex);
					}
				}

			}

			HashMap tmpCheckMap = new HashMap();
			ok_servers = LoadBalanceFactory.getInstance().getArivableServers();
			for (int i = 0; i < ok_servers.length; i++) {
				SocketObjectPool objSocketObjectPool = null;
				Socket socket = null;
				try {
					objSocketObjectPool = (SocketObjectPool) ok_servers[i];
					socket = (Socket) objSocketObjectPool.borrowObject();
					HashMap tmp = driver.stats(socket);
					String host = (String) tmp.get("host");
					String uptime = (String) tmp.get("uptime");
					String cmdFlush = (String) tmp.get("cmd_flush");

					LocalCacheCheckObject checkObj = new LocalCacheCheckObject();
					if ((!StringUtils.isBlank(uptime)) && (StringUtils.isNumeric(uptime))) {
						checkObj.setUpTime(System.currentTimeMillis() - Long.parseLong(uptime) * 1000L);
					}
					if ((!StringUtils.isBlank(cmdFlush)) && (StringUtils.isNumeric(cmdFlush))) {
						checkObj.setCmdFlush(Long.parseLong(cmdFlush));
					}

					tmpCheckMap.put(host, checkObj);
				} catch (Exception ex) {
					log.info("easymem local cache定时检查stats出错", ex);
				} finally {
					if ((socket != null) && (objSocketObjectPool != null)) {
						objSocketObjectPool.returnObject(socket);
					}
				}

			}

			if ((this.checkSnapshot == null) || (this.checkSnapshot.isEmpty())) {
				this.checkSnapshot.putAll(tmpCheckMap);
			} else {
				Set keys = this.checkSnapshot.keySet();
				for (Iterator iter = keys.iterator(); iter.hasNext();) {
					String host = (String) iter.next();
					LocalCacheCheckObject snapShot = (LocalCacheCheckObject) this.checkSnapshot.get(host);
					if (tmpCheckMap.containsKey(host)) {
						LocalCacheCheckObject now = (LocalCacheCheckObject) tmpCheckMap.get(host);

						if ((Math.abs(snapShot.getUpTime() - now.getUpTime()) >= this.statDiffTime * 1000) || (snapShot.getCmdFlush() != now.getCmdFlush())) {
							MemcachedBufferedDriverLocalCache.LOCAL_CACHE.clear();
							MemcachedBufferedDriverLocalCache.LAST_LOCAL_CACHE_CLEAR_TIME = System.currentTimeMillis();

							if (log.isInfoEnabled()) {
								log.info("清除easymem local cache");
							}

							this.checkSnapshot.clear();
							return;
						}

						if (log.isInfoEnabled())
							log.info("时间差异小于" + this.statDiffTime + "秒,基准时间:" + snapShot.getUpTime() + ",比较时间:" + now.getUpTime() + ",cmd_flush" + ",基准:"
									+ snapShot.getCmdFlush() + ",现在:" + now.getCmdFlush() + ",相等");
					}
				}
			}
		} catch (Throwable ex) {
			log.error("easymem local cache定时检查memcached出错", ex);
		}
	}

	public static void main(String[] args) throws Exception {
		DefaultEasyMemClient.getInstance().get("1");
		Timer timer = new Timer();
		timer.schedule(new LocalCacheTask(new String[] { "aaaa", "2:02", "2:03", "2:04", "2:05" }, 300), 10L, 5000L);
	}
}