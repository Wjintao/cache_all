package com.asiainfo.easymem.driver.io;

import java.io.IOException;

/**
 * ���л��Ľӿ�
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public interface ISerializable {
	/** ����ת���ֽ����� */
	public byte[] object2bytes(Object object) throws IOException;

	/** �ֽ�����ת�ɶ��� */
	public Object bytes2object(byte[] bytes) throws IOException, ClassNotFoundException;
}
