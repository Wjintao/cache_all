package com.asiainfo.easymem.exception;

public class SocketFlushException extends Exception {
	public SocketFlushException() {
		super();
	}

	public SocketFlushException(String message) {
		super(message);
	}

	public SocketFlushException(String message, Throwable cause) {
		super(message, cause);
	}

	public SocketFlushException(Throwable cause) {
		super(cause);
	}
}
