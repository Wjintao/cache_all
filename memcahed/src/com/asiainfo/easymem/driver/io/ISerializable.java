package com.asiainfo.easymem.driver.io;

import java.io.IOException;

/**
 * 序列化的接口
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public interface ISerializable {
	/** 对象转成字节数组 */
	public byte[] object2bytes(Object object) throws IOException;

	/** 字节数组转成对象 */
	public Object bytes2object(byte[] bytes) throws IOException, ClassNotFoundException;
}
