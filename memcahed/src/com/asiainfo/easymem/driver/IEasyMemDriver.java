package com.asiainfo.easymem.driver;

import java.net.Socket;
import java.util.Map;
import java.util.HashMap;

/**
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public interface IEasyMemDriver {

	public boolean setKeyAndValue2AllServer(String key, Object obj) throws Exception;

	public boolean set(String key, Object obj) throws Exception;

	/** ����key���value */
	public Object get(String key) throws Exception;

	/** ����key���value */
	public Map get(String[] key) throws Exception;

	/** ���ֵΪ����֤ */
	public Object get(Socket socket, String key) throws Exception;

	/** ��ö��ֵ */
	public Map get(Socket socket, String[] key) throws Exception;

	/** ����keyɾ��value */
	public boolean delete(String key) throws Exception;

	/** ���״̬��Ϣ */
	public HashMap stats() throws Exception;
}
