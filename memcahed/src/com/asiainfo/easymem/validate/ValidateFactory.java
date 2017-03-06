package com.asiainfo.easymem.validate;

import java.net.Socket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.EasyMemConfigure;

import java.util.Properties;
import org.apache.commons.lang.StringUtils;

/**
 * ��֤����
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class ValidateFactory {
	private transient static Log log = LogFactory.getLog(ValidateFactory.class);

	private ValidateFactory() {
	}

	public static boolean validate(Socket socket) {
		if (socket == null || !socket.isConnected()) {
			log.error("�����Ѿ��رջ������Ӳ�����");
			return false;
		}

		boolean rtn = false;
		try {
			Properties properties = EasyMemConfigure.getProperties("server.validate", true);
			if (properties == null || properties.size() == 0) {
				// ����Ҫ��֤
				rtn = true;
			} else {
				if (!StringUtils.isBlank(properties.getProperty("class"))) {
					IValidate objIValidate = (IValidate) Class.forName(properties.getProperty("class").trim()).newInstance();
					rtn = objIValidate.validate(socket, properties);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("����Ҫ��֤");
					}
					rtn = true;
				}
			}
		} catch (Exception ex) {
			log.error("��֤��������", ex);
			rtn = false;
		}
		return rtn;
	}

}
