package com.asiainfo.easymem.driver.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.asiainfo.easymem.driver.io.serialization.hessian.io.AbstractHessianInput;
import com.asiainfo.easymem.driver.io.serialization.hessian.io.AbstractHessianOutput;
import com.asiainfo.easymem.driver.io.serialization.hessian.io.Hessian2Input;
import com.asiainfo.easymem.driver.io.serialization.hessian.io.Hessian2Output;
import com.asiainfo.easymem.driver.io.serialization.hessian.io.SerializerFactory;

/**
 * hessian的序列化
 * 
 * @author linzhaoming
 * 
 *         Created at 2013-6-4
 */
public class HessianSerializable implements ISerializable {
	private static final SerializerFactory SF = new SerializerFactory();

	/** 对象转成字节数组 */
	public byte[] object2bytes(Object object) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();

		AbstractHessianOutput out = new Hessian2Output(b);
		;
		out.setSerializerFactory(SF);
		out.startReply();
		out.writeObject(object);
		out.completeReply();
		out.flush();

		return b.toByteArray();
	}

	/** 字节数组转成对象 */
	public Object bytes2object(byte[] bytes) throws IOException, ClassNotFoundException {
		Object rtn = null;
		try {
			ByteArrayInputStream b = new ByteArrayInputStream(bytes);
			AbstractHessianInput in = new Hessian2Input(b);
			;
			in.setSerializerFactory(SF);
			in.startReply();
			rtn = in.readObject();
			in.completeReply();
			b.close();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
		return rtn;
	}
}
