package com.asiainfo.easymem.driver.map;

public class SizeObject {
	private Object obj = null;
	private int size = 0;

	public SizeObject(Object obj, int size) {
		this.obj = obj;
		this.size = size;
	}

	public Object getObj() {
		return this.obj;
	}

	public int getSize() {
		return this.size;
	}
}