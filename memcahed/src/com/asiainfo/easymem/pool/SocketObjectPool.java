package com.asiainfo.easymem.pool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.asiainfo.easymem.EasyMemConfigure;

public class SocketObjectPool extends GenericObjectPool implements ObjectPool {
	private transient static Log log = LogFactory.getLog(SocketObjectPool.class);

	private static int maxIdle = 8;
	private static int minIdle = 8;
	private static int maxActive = 8;
	private static int maxWait = -1;

	static {
		// ���������С�͵ȴ�����
		try {
			String strMin = EasyMemConfigure.getProperties("server.conn", true).getProperty("min");
			String strMax = EasyMemConfigure.getProperties("server.conn", true).getProperty("max");
			minIdle = Integer.parseInt(strMin);
			maxIdle = Integer.parseInt(strMax);
			maxActive = maxIdle;
		} catch (Throwable ex) {
			log.error("ת������,ȡĬ������", ex);
			maxIdle = 8;
			minIdle = 8;
			maxActive = 8;
			maxWait = -1;
		}

	}

	private SocketPoolableObjectFactory objFactory = null;

	/**
	 * 
	 * @param objFactory
	 *            PoolableObjectFactory
	 */
	public SocketObjectPool(SocketPoolableObjectFactory objFactory) {
		super(objFactory);
		this.setMaxIdle(maxIdle);
		this.setMinIdle(minIdle);
		this.setMaxActive(maxActive);
		this.setMaxWait(maxWait);

		this.objFactory = objFactory;
	}

	public String getHost() {
		return this.objFactory.getHost();
	}

	public int getPort() {
		return this.objFactory.getPort();
	}

	public int getTimeoutSeconds() {
		return this.objFactory.getTimeoutSeconds();
	}

	public String toString() {
		return "[host=" + this.objFactory.getHost() + ",port=" + this.objFactory.getPort() + "]SocketObjectPool";
	}

}
