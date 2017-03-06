package com.asiainfo.easymem.policy;

/**
 * ·þÎñ
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class Server {
	private String host = null;
	private int port = 0;
	private int timeoutSeconds = 0;

	public Server(String host, int port, int timeoutSeconds) {
		this.host = host;
		this.port = port;
		this.timeoutSeconds = timeoutSeconds;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}
}
