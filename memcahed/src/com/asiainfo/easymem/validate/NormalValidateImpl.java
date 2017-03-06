package com.asiainfo.easymem.validate;

import java.net.Socket;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.DefaultEasyMemClient;
import com.asiainfo.easymem.driver.IEasyMemDriver;

/**
 * 正常验证
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class NormalValidateImpl implements IValidate {
	private transient static Log log = LogFactory.getLog(NormalValidateImpl.class);

	public NormalValidateImpl() {
	}

	public boolean validate(Socket socket, Properties properties) throws Exception {
		boolean rtn = false;
		try {
			String key = properties.getProperty("key");
			String value1 = properties.getProperty("value");

			IEasyMemDriver objEasyMemDriver = (IEasyMemDriver) Class.forName(DefaultEasyMemClient.DRIVER_CLASS_NAME).newInstance();
			String value2 = (String) objEasyMemDriver.get(socket, key);
			if (value2 == null) {
				throw new Exception("在easymem中没有设置对应的key的value");
			} else if (value2.equals(value1)) {
				rtn = true;
			}
		} catch (Exception ex) {
			log.error("验证出现错误", ex);
			throw ex;
		}
		return rtn;
	}

}
