package com.asiainfo.easymem.validate;

import java.net.Socket;
import java.util.Properties;

/**
 * ����Ҫ��֤��ʵ��
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
