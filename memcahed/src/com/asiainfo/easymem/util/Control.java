package com.asiainfo.easymem.util;

import com.asiainfo.easymem.DefaultEasyMemClient;

public class Control {
	public Control() {
	}

	public static void main(String[] args) throws Exception {
		DefaultEasyMemClient objDefaultEasyMemClient = DefaultEasyMemClient.getInstance();

		try {
			System.out.println(objDefaultEasyMemClient.get("validate.IsReady"));
		} catch (Throwable ex) {
			ex.printStackTrace();
		}

		objDefaultEasyMemClient.setKeyAndValue2AllServer("validate.IsReady", "OK");
	}

}
