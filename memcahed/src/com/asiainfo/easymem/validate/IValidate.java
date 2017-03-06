package com.asiainfo.easymem.validate;

import java.net.Socket;
import java.util.Properties;

public interface IValidate {

	/** —È÷§ */
	public boolean validate(Socket socket, Properties properties) throws Exception;
}
