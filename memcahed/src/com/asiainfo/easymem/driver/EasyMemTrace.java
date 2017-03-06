package com.asiainfo.easymem.driver;

import java.util.Date;

import com.ai.appframe2.complex.trace.ITrace;
import com.ai.appframe2.complex.trace.TraceUtil;

public class EasyMemTrace implements ITrace {
	public static final String PROCESS_METHOD_GET = "GET";
	public static final String PROCESS_METHOD_SET = "SET";

	private long createTime = 0;
	private String host = null;
	private Object[] in = null;
	private String center = null;
	private String code = null;
	private boolean success = false;
	private int useTime = 0;
	private int getTime = 0;
	private int count = 0;
	private String processMethod = null;

	public EasyMemTrace() {
	}

	public void addChild(ITrace objITrace) {
	}

	public String toXml() {
		StringBuffer sb = new StringBuffer();
		sb.append("<easymem id=\"" + TraceUtil.getTraceId() + "\" time=\"" + ITrace.DATE_FORMAT.format(new Date(this.createTime)) + "\">");
		sb.append("<host>" + this.getHost() + "</host>");
		sb.append("<cen>" + this.getCenter() + "</cen>");
		sb.append("<code>" + this.getCode() + "</code>");

		if (this.in != null && this.in.length > 0) {
			sb.append("<in>" + TraceUtil.object2xml(this.in) + "</in>");
		}

		sb.append("<c>" + this.count + "</c>");
		sb.append("<pm>" + this.processMethod + "</pm>");

		if (this.isSuccess()) {
			sb.append("<s>1</s>");
		} else {
			sb.append("<s>0</s>");
		}
		sb.append("<et>" + this.getUseTime() + "</et>");
		sb.append("<gt>" + this.getGetTime() + "</gt>");

		sb.append("</easymem>");

		return sb.toString();
	}

	public String getCenter() {
		return center;
	}

	public String getCode() {
		return code;
	}

	public long getCreateTime() {
		return createTime;
	}

	public Object[] getIn() {
		return in;
	}

	public boolean isSuccess() {
		return success;
	}

	public int getUseTime() {
		return useTime;
	}

	public void setUseTime(int useTime) {
		this.useTime = useTime;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public void setIn(Object[] in) {
		this.in = in;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setCenter(String center) {
		this.center = center;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getGetTime() {
		return getTime;
	}

	public String getProcessMethod() {
		return processMethod;
	}

	public void setGetTime(int getTime) {
		this.getTime = getTime;
	}

	public void setProcessMethod(String processMethod) {
		this.processMethod = processMethod;
	}
}
