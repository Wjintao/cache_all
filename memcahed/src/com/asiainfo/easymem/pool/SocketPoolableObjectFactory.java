package com.asiainfo.easymem.pool;

import java.net.*;

import org.apache.commons.logging.*;
import org.apache.commons.pool.*;

import com.asiainfo.easymem.validate.*;

public class SocketPoolableObjectFactory implements PoolableObjectFactory {
	private transient static Log log = LogFactory.getLog(SocketPoolableObjectFactory.class);

	private String host = null;
	private int port = 0;
	private int timeoutSeconds = 0;

	public SocketPoolableObjectFactory(String host, int port, int timeoutSeconds) {
		this.host = host;
		this.port = port;
		this.timeoutSeconds = timeoutSeconds;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	/**
	 * 制造对象
	 * 
	 * @return Object
	 * @throws Exception
	 */
	public Object makeObject() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("开始构造对象");
		}

		Socket socket = null;

		// 1、建立连接
		try {
			socket = new Socket();
			socket.setTcpNoDelay(true);
			socket.setKeepAlive(true);
			socket.connect(new InetSocketAddress(InetAddress.getByName(this.host), this.port), this.timeoutSeconds * 1000);
			socket.setSoTimeout(this.timeoutSeconds * 1000);
		} catch (Exception ex) {
			log.error("构造socket对象失败", ex);
			throw ex;
		}

		// 2、验证连接
		try {
			if (!ValidateFactory.validate(socket)) {
				throw new Exception("Socket:" + socket + ",验证失败");
			}
		} catch (Exception ex) {
			if (socket != null) {
				socket.close();
				socket = null;
			}
			throw ex;
		}

		return socket;
	}

	/**
	 * 销毁对象
	 * 
	 * @param object
	 *            Object
	 * @throws Exception
	 */
	public void destroyObject(Object object) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("销毁对象:" + object);
		}
		if (object != null && object instanceof Socket) {
			((Socket) object).close();
		}
	}

	/**
	 * 验证对象
	 * 
	 * @param object
	 *            Object
	 * @return boolean
	 */
	public boolean validateObject(Object object) {
		if (log.isDebugEnabled()) {
			log.debug("验证对象");
		}
		return true;
	}

	/**
	 * 激活对象
	 * 
	 * @param object
	 *            Object
	 * @throws Exception
	 */
	public void activateObject(Object object) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("激活对象:" + object);
		}
	}

	/**
	 * 去激活对象
	 * 
	 * @param object
	 *            Object
	 * @throws Exception
	 */
	public void passivateObject(Object object) throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("去激活对象:" + object);
		}
	}

}
