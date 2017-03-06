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
public class BasicValidateImpl implements IValidate {
	private transient static Log log = LogFactory.getLog(BasicValidateImpl.class);

	public BasicValidateImpl() {
	}

	public boolean validate(Socket socket, Properties properties) throws Exception {
		boolean rtn = false;
		try {
			IEasyMemDriver objEasyMemDriver = (IEasyMemDriver) Class.forName(DefaultEasyMemClient.DRIVER_CLASS_NAME).newInstance();
			objEasyMemDriver.get(socket, "test");

			// 只要socket可以连接就可以了，不要验证指定的key
			rtn = true;
		} catch (Exception ex) {
			log.error("验证出现错误", ex);
			throw ex;
		}
		return rtn;
	}

}
