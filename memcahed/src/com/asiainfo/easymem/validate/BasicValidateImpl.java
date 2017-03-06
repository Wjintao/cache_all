package com.asiainfo.easymem.validate;

import java.net.Socket;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.asiainfo.easymem.DefaultEasyMemClient;
import com.asiainfo.easymem.driver.IEasyMemDriver;

/**
 * ������֤
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

			// ֻҪsocket�������ӾͿ����ˣ���Ҫ��ָ֤����key
			rtn = true;
		} catch (Exception ex) {
			log.error("��֤���ִ���", ex);
			throw ex;
		}
		return rtn;
	}

}
