package com.asiainfo.easymem.driver.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ��׼java�����л�
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class JavaSerializable implements ISerializable {

	/** ����ת���ֽ����� */
	public byte[] object2bytes(Object o) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		new ObjectOutputStream(b).writeObject(o);
		return b.toByteArray();
	}

	/** �ֽ�����ת�ɶ��� */
	public Object bytes2object(byte[] bytes) throws IOException, ClassNotFoundException {
		return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
	}
}
