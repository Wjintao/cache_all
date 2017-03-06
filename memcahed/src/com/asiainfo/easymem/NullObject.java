package com.asiainfo.easymem;

import java.io.Serializable;

/**
 * ø’∂‘œÛ
 * 
 * @author linzhaoming
 * 
 * Created at 2012-10-15
 */
public final class NullObject implements Serializable {
	public static final NullObject NULL = new NullObject();

	private NullObject() {
	}
}
