package com.asiainfo.easymem.exception;

/**
 * ������failover���쳣
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class NormalException extends Exception {
	public NormalException() {
		super();
	}

	public NormalException(String message) {
		super(message);
	}

	public NormalException(String message, Throwable cause) {
		super(message, cause);
	}

	public NormalException(Throwable cause) {
		super(cause);
	}
}
