package com.asiainfo.easymem.validate;

import java.net.Socket;
import java.util.Properties;

/**
 * 不需要验证的实现
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class NoNeedValidateImpl implements IValidate {
	public NoNeedValidateImpl() {
	}

	public boolean validate(Socket socket, Properties properties) throws Exception {
		return true;
	}
}
