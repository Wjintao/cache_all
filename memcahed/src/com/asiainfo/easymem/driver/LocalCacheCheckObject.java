package com.asiainfo.easymem.driver;

import java.io.Serializable;

public class LocalCacheCheckObject implements Serializable {
	/** Uptimeʱ��, STAT uptime 251601*/
	private long upTime = 0L;
	/** Flush������ STAT cmd_flush 0*/
	private long cmdFlush = 0L;

	/** Flush������ STAT cmd_flush 0*/
	public long getCmdFlush() {
		return this.cmdFlush;
	}

	/** Uptimeʱ��, STAT uptime 251601*/
	public long getUpTime() {
		return this.upTime;
	}

	/** Uptimeʱ��, STAT uptime 251601*/
	public void setUpTime(long upTime) {
		this.upTime = upTime;
	}

	/** Flush������ STAT cmd_flush 0*/
	public void setCmdFlush(long cmdFlush) {
		this.cmdFlush = cmdFlush;
	}
}