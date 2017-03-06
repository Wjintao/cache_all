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

	/** 根据key获得value */
	public Object get(String key) throws Exception;

	/** 根据key获得value */
	public Map get(String[] key) throws Exception;

	/** 获得值为了验证 */
	public Object get(Socket socket, String key) throws Exception;

	/** 获得多个值 */
	public Map get(Socket socket, String[] key) throws Exception;

	/** 根据key删除value */
	public boolean delete(String key) throws Exception;

	/** 获得状态信息 */
	public HashMap stats() throws Exception;
}
