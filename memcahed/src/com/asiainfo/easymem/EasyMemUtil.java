package com.asiainfo.easymem;

import org.apache.commons.lang.StringUtils;

public final class EasyMemUtil {
	private static Boolean INIT = Boolean.FALSE;
	private static boolean IS_ENABLE = false;

	private EasyMemUtil() {
	}

	public static boolean isEnable() {
		if (INIT.equals(Boolean.FALSE)) {
			synchronized (INIT) {
				if (INIT.equals(Boolean.FALSE)) {
					String str = EasyMemConfigure.getProperties().getProperty("is_enable");
					if (!StringUtils.isBlank(str) && str.trim().equalsIgnoreCase("true")) {
						IS_ENABLE = true;
					}
					INIT = Boolean.TRUE;
				}
			}
		}

		return IS_ENABLE;
	}

}
