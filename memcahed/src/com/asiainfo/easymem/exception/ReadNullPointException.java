package com.asiainfo.easymem.exception;

public class ReadNullPointException extends Exception {
	public ReadNullPointException() {
		super();
	}

	public ReadNullPointException(String message) {
		super(message);
	}

	public ReadNullPointException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReadNullPointException(Throwable cause) {
		super(cause);
	}
}
