package com.asiainfo.easymem.driver;

import java.io.Serializable;

public class LocalCacheCheckObject implements Serializable {
	/** Uptime时间, STAT uptime 251601*/
	private long upTime = 0L;
	/** Flush的数量 STAT cmd_flush 0*/
	private long cmdFlush = 0L;

	/** Flush的数量 STAT cmd_flush 0*/
	public long getCmdFlush() {
		return this.cmdFlush;
	}

	/** Uptime时间, STAT uptime 251601*/
	public long getUpTime() {
		return this.upTime;
	}

	/** Uptime时间, STAT uptime 251601*/
	public void setUpTime(long upTime) {
		this.upTime = upTime;
	}

	/** Flush的数量 STAT cmd_flush 0*/
	public void setCmdFlush(long cmdFlush) {
		this.cmdFlush = cmdFlush;
	}
}