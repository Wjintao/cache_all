package com.asiainfo.easymem.validate;

import java.net.Socket;
import java.util.Properties;

public interface IValidate {

	/** ��֤ */
	public boolean validate(Socket socket, Properties properties) throws Exception;
}
