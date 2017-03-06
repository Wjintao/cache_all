package com.asiainfo.easymem.driver.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 标准java的序列化
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class JavaSerializable implements ISerializable {

	/** 对象转成字节数组 */
	public byte[] object2bytes(Object o) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		new ObjectOutputStream(b).writeObject(o);
		return b.toByteArray();
	}

	/** 字节数组转成对象 */
	public Object bytes2object(byte[] bytes) throws IOException, ClassNotFoundException {
		return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
	}
}
