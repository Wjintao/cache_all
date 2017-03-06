package com.asiainfo.easymem.driver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ai.appframe2.common.SessionManager;
import com.ai.appframe2.complex.center.CenterFactory;
import com.ai.appframe2.complex.trace.TraceFactory;
import com.ai.appframe2.privilege.UserInfoInterface;
import com.asiainfo.easymem.EasyMemConfigure;
import com.asiainfo.easymem.driver.io.IOFactory;
import com.asiainfo.easymem.driver.listener.EasyMemTransactionFactory;
import com.asiainfo.easymem.exception.NormalException;
import com.asiainfo.easymem.exception.ReadNullPointException;
import com.asiainfo.easymem.exception.SocketFlushException;
import com.asiainfo.easymem.policy.LoadBalanceFactory;
import com.asiainfo.easymem.policy.Server;
import com.asiainfo.easymem.pool.SocketObjectPool;

/**
 * easymem的驱动
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class EasyMemBufferedDriver implements IEasyMemDriver {
	private transient static Log log = LogFactory.getLog(EasyMemBufferedDriver.class);

	// 压缩标志
	private static final int F_COMPRESSED = 2;

	// 字符串的字节
	public static final byte[] BYTE_GET = new byte[] { 103, 101, 116, 32 };
	public static final byte[] BYTE_SET = new byte[] { 115, 101, 116, 32 };
	public static final byte[] BYTE_DELETE = new byte[] { 100, 101, 108, 101, 116, 101, 32 };
	public static final byte[] BYTE_CRLF = new byte[] { 13, 10 };
	public static final byte[] BYTE_SPACE = new byte[] { 32 };

	public static final String SERVER_STATUS_DELETED = "DELETED";
	public static final String SERVER_STATUS_NOT_FOUND = "NOT_FOUND";
	public static final String SERVER_STATUS_STORED = "STORED";
	public static final String SERVER_STATUS_ERROR = "ERROR";
	public static final String SERVER_STATUS_END = "END";
	public static final String SERVER_STATUS_VALUE = "VALUE";

	public static final String ENCODING_TYPE = "UTF-8";

	// 最大字节数
	public static int MAX_BYTE_SIZE = 5 * 1024 * 1024;
	// 多大字节开始压缩
	public static int COMPRESS_THRESHOLD = 100 * 1024;

	static {
		try {
			String compress = EasyMemConfigure.getProperties().getProperty("server.compress_threshold");
			if (!StringUtils.isBlank(compress) && StringUtils.isNumeric(compress)) {
				COMPRESS_THRESHOLD = Integer.parseInt(compress);
			}

			String max = EasyMemConfigure.getProperties().getProperty("server.server.max_byte_size");
			if (!StringUtils.isBlank(max) && StringUtils.isNumeric(max)) {
				MAX_BYTE_SIZE = Integer.parseInt(max);
			}
		} catch (Exception ex) {
			log.error("获得压缩阀值或者最大字节数出错,采用默认值,不影响系统运行", ex);
		}
	}

	/** 设置key和value到所有的server */
	public boolean setKeyAndValue2AllServer(String key, Object obj) throws Exception {
		if (obj == null || key == null || "".equals(key)) {
			throw new Exception("key和value不能为空");
		}

		// 是否打开trace
		long start = 0;
		EasyMemTrace objEasyMemTrace = null;
		if (TraceFactory.isEnableTrace()) {
			objEasyMemTrace = new EasyMemTrace();
			start = System.currentTimeMillis();
			objEasyMemTrace.setCreateTime(start);
			if (CenterFactory.isSetCenterInfo()) {
				objEasyMemTrace.setCenter(CenterFactory.getCenterInfo().getRegion() + "," + CenterFactory.getCenterInfo().getCenter());
			}

			UserInfoInterface user = SessionManager.__getUserWithOutLog();
			if (user != null && user.getCode() != null) {
				objEasyMemTrace.setCode(user.getCode());
			}

			objEasyMemTrace.setIn(new Object[] { key });
		}
		// 打开trace结束

		boolean rtn = false;

		// 仅仅一次对key进行编码，不能在循环中对key进行编码，否则重复编码后对应的key发生了变化
		key = encodeKey(key);

		Object[] arivableServers = LoadBalanceFactory.getInstance().getArivableServers();
		for (int i = 0; i < arivableServers.length; i++) {
			SocketObjectPool objSocketObjectPool = (SocketObjectPool) arivableServers[i];

			Socket socket = null;
			try {
				// trace
				long getTimeStart = 0;
				if (objEasyMemTrace != null) {
					getTimeStart = System.currentTimeMillis();
				}

				socket = (Socket) objSocketObjectPool.borrowObject();

				// trace
				if (objEasyMemTrace != null) {
					if (socket != null && socket.getRemoteSocketAddress() != null) {
						InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();

						// 汇总地址
						if (StringUtils.isBlank(objEasyMemTrace.getHost())) {
							objEasyMemTrace.setHost(address.getHostName() + ":" + address.getPort());
						} else {
							objEasyMemTrace.setHost(objEasyMemTrace.getHost() + "," + address.getHostName() + ":" + address.getPort());
						}

						// 汇总时间
						if (getTimeStart > 0) {
							objEasyMemTrace.setGetTime(objEasyMemTrace.getGetTime() + ((int) (System.currentTimeMillis() - getTimeStart)));
						}
					}
				}

				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

				int flag = 0;

				// 序列化对象
				byte[] bs = object2Byte(obj);

				// 压缩
				if (bs.length > COMPRESS_THRESHOLD) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream(bs.length);
					GZIPOutputStream gos = new GZIPOutputStream(bos);
					gos.write(bs, 0, bs.length);
					gos.finish();

					bs = bos.toByteArray();
					flag |= F_COMPRESSED;
				}
				// 压缩结束

				// 最大字节限制
				if (bs.length >= MAX_BYTE_SIZE) {
					throw new NormalException("不能超过" + MAX_BYTE_SIZE + "字节");
				}

				out.write(EasyMemBufferedDriver.BYTE_SET); // write cmd
				out.write(key.getBytes()); // write key
				out.write(EasyMemBufferedDriver.BYTE_SPACE);
				out.write(String.valueOf(flag).getBytes()); // write flag
				out.write(EasyMemBufferedDriver.BYTE_SPACE);
				out.write("0".getBytes()); // write expire date
				out.write(EasyMemBufferedDriver.BYTE_SPACE);
				out.write(String.valueOf(bs.length).getBytes()); // object
																	// length
				out.write(EasyMemBufferedDriver.BYTE_CRLF);

				out.write(bs);
				out.write(EasyMemBufferedDriver.BYTE_CRLF);
				out.flush();

				String ret = readLine(in);
				rtn = EasyMemBufferedDriver.SERVER_STATUS_STORED.equals(ret);
				if (!rtn) {
					throw new Exception("set出现错误:" + ret);
				}

				// trace
				if (objEasyMemTrace != null) {
					objEasyMemTrace.setSuccess(true);
					// 汇总时间
					objEasyMemTrace.setUseTime(objEasyMemTrace.getUseTime() + (int) (System.currentTimeMillis() - start));
					objEasyMemTrace.setProcessMethod(EasyMemTrace.PROCESS_METHOD_SET);

					// 如果是最后一个
					if (i == (arivableServers.length - 1)) {
						TraceFactory.addTraceInfo(objEasyMemTrace);
					}
				}
			} catch (Exception ex) {
				if (!(ex instanceof NormalException)) {
					log.error("发生异常，删除连接池:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
				}

				// 因为权限cache的set不成功不会引起业务问题
				// 不抛出异常，继续循环处理

				// trace
				if (objEasyMemTrace != null) {
					objEasyMemTrace.setSuccess(false);
					// 汇总时间
					objEasyMemTrace.setUseTime(objEasyMemTrace.getUseTime() + (int) (System.currentTimeMillis() - start));
					TraceFactory.addTraceInfo(objEasyMemTrace);
				}

			} finally {
				if (socket != null && objSocketObjectPool != null) {
					objSocketObjectPool.returnObject(socket);
				}
			}
		}

		return rtn;
	}

	/**
	 * 设置key和value
	 * 
	 * @param key
	 *            String
	 * @param obj
	 *            Object
	 * @throws Exception
	 * @return boolean
	 */
	public boolean set(String key, Object obj) throws Exception {
		if (obj == null || key == null || "".equals(key)) {
			throw new Exception("key和value不能为空");
		}

		boolean rtn = false;

		Socket socket = null;
		SocketObjectPool objSocketObjectPool = null;
		try {
			objSocketObjectPool = LoadBalanceFactory.getInstance().getSocketObjectPool();
			socket = (Socket) objSocketObjectPool.borrowObject();

			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

			key = encodeKey(key);
			int flag = 0;

			// 序列化对象
			byte[] bs = object2Byte(obj);

			// 压缩
			if (bs.length > COMPRESS_THRESHOLD) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream(bs.length);
				GZIPOutputStream gos = new GZIPOutputStream(bos);
				gos.write(bs, 0, bs.length);
				gos.finish();

				bs = bos.toByteArray();
				flag |= F_COMPRESSED;
			}
			// 压缩结束

			// 最大字节限制
			if (bs.length >= MAX_BYTE_SIZE) {
				throw new NormalException("不能超过" + MAX_BYTE_SIZE + "字节");
			}

			out.write(EasyMemBufferedDriver.BYTE_SET); // write cmd
			out.write(key.getBytes()); // write key
			out.write(EasyMemBufferedDriver.BYTE_SPACE);
			out.write(String.valueOf(flag).getBytes()); // write flag
			out.write(EasyMemBufferedDriver.BYTE_SPACE);
			out.write("0".getBytes()); // write expire date
			out.write(EasyMemBufferedDriver.BYTE_SPACE);
			out.write(String.valueOf(bs.length).getBytes()); // object length
			out.write(EasyMemBufferedDriver.BYTE_CRLF);

			out.write(bs);
			out.write(EasyMemBufferedDriver.BYTE_CRLF);
			out.flush();

			String ret = readLine(in);
			rtn = EasyMemBufferedDriver.SERVER_STATUS_STORED.equals(ret);
			if (!rtn) {
				throw new Exception("set出现错误:" + ret);
			}
		} catch (Exception ex) {
			if (!(ex instanceof NormalException)) {
				log.error("发生异常，删除连接池:", ex);
				LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
			}
			throw ex;
		} finally {
			if (socket != null && objSocketObjectPool != null) {
				objSocketObjectPool.returnObject(socket);
			}
		}
		return rtn;
	}

	/**
	 * 根据key获得value
	 * 
	 * @param key
	 *            String
	 * @throws Exception
	 * @return Object
	 */
	public Object get(String key) throws Exception {
		Object rtn = null;
		if (EasyMemTransactionFactory.existTransaction()) {
			rtn = EasyMemTransactionFactory.getFromCache(key);
			if (rtn == null) {
				rtn = this._get(key);
				EasyMemTransactionFactory.putIntoCache(key, rtn);
			} else {
				if (rtn.equals(EasyMemTransactionFactory.NULL_OBJECT)) {
					rtn = null;
				}
			}
		} else {
			rtn = this._get(key);
		}
		return rtn;
	}

	/**
	 * 根据key获得value
	 * 
	 * @param key
	 *            String
	 * @throws Exception
	 * @return Object
	 */
	private Object _get(String key) throws Exception {
		Object rtn = null;

		long start = 0;
		// 是否打开trace
		EasyMemTrace objMemTrace = null;
		if (TraceFactory.isEnableTrace()) {
			objMemTrace = new EasyMemTrace();
			start = System.currentTimeMillis();
			objMemTrace.setCreateTime(start);
			if (CenterFactory.isSetCenterInfo()) {
				objMemTrace.setCenter(CenterFactory.getCenterInfo().getRegion() + "," + CenterFactory.getCenterInfo().getCenter());
			}

			UserInfoInterface user = SessionManager.__getUserWithOutLog();
			if (user != null && user.getCode() != null) {
				objMemTrace.setCode(user.getCode());
			}

			objMemTrace.setIn(new Object[] { key });
		}
		// 打开trace结束

		Socket socket = null;
		SocketObjectPool objSocketObjectPool = null;
		try {
			// trace
			long getTimeStart = 0;
			if (objMemTrace != null) {
				getTimeStart = System.currentTimeMillis();
			}

			objSocketObjectPool = LoadBalanceFactory.getInstance().getSocketObjectPool();
			socket = (Socket) objSocketObjectPool.borrowObject();

			// trace
			if (objMemTrace != null) {
				if (socket != null && socket.getRemoteSocketAddress() != null) {
					InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();
					objMemTrace.setHost(address.getHostName() + ":" + address.getPort());
					if (getTimeStart > 0) {
						objMemTrace.setGetTime((int) (System.currentTimeMillis() - getTimeStart));
					}
				}
			}

			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			key = encodeKey(key);
			out.write(EasyMemBufferedDriver.BYTE_GET);
			out.write(key.getBytes());
			out.write(EasyMemBufferedDriver.BYTE_CRLF);

			// 对于采用连接池，当easymem重启后，客户端还是保留的原来的socket的句柄
			// 当有业务访问的时候就出现了SocketFlushException和ReadNullPointException异常
			// 只需要立即删除和重建连接池
			// 如果这个时候easymem不存在了,但是客户端还是继续保留了socket句柄，会经历重试次数后，就返回失败
			try {
				out.flush();
			} catch (Exception ex) {
				throw new SocketFlushException(ex);
			}

			rtn = getObjectFromStream(in, out);

			// trace
			if (objMemTrace != null) {
				objMemTrace.setSuccess(true);
				objMemTrace.setUseTime((int) (System.currentTimeMillis() - start));
				objMemTrace.setProcessMethod(EasyMemTrace.PROCESS_METHOD_GET);
				if (rtn != null) {
					if (rtn.getClass().isArray()) {
						objMemTrace.setCount(Array.getLength(rtn));
					} else if (rtn instanceof Map) {
						objMemTrace.setCount(((Map) rtn).size());
					} else if (rtn instanceof List) {
						objMemTrace.setCount(((List) rtn).size());
					} else {
						objMemTrace.setCount(1);
					}
				}
				TraceFactory.addTraceInfo(objMemTrace);
			}
		} catch (Exception ex) {
			if (ex instanceof ReadNullPointException) {
				String host = objSocketObjectPool.getHost();
				int port = objSocketObjectPool.getPort();
				int timeoutSeconds = objSocketObjectPool.getTimeoutSeconds();
				synchronized (this) {
					log.error("发生ReadNullPointException异常，删除连接池:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
					log.error("立即建立连接池[" + host + ":" + port + "]");
					LoadBalanceFactory.getInstance().addPool(LoadBalanceFactory.getInstance().makePool(new Server(host, port, timeoutSeconds)));
				}
			} else if (ex instanceof SocketFlushException) {
				String host = objSocketObjectPool.getHost();
				int port = objSocketObjectPool.getPort();
				int timeoutSeconds = objSocketObjectPool.getTimeoutSeconds();
				synchronized (this) {
					log.error("发生SocketFlushException异常，删除连接池:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
					log.error("立即建立连接池[" + host + ":" + port + "]");
					LoadBalanceFactory.getInstance().addPool(LoadBalanceFactory.getInstance().makePool(new Server(host, port, timeoutSeconds)));
				}
			} else {
				log.error("发生异常，删除连接池:", ex);
				LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
			}

			// trace
			if (objMemTrace != null) {
				objMemTrace.setSuccess(false);
				objMemTrace.setUseTime((int) (System.currentTimeMillis() - start));
				objMemTrace.setProcessMethod(EasyMemTrace.PROCESS_METHOD_GET);
				TraceFactory.addTraceInfo(objMemTrace);
			}

			throw ex;
		} finally {
			if (socket != null && objSocketObjectPool != null) {
				objSocketObjectPool.returnObject(socket);
			}
		}
		return rtn;
	}

	/**
	 * 根据key获得value
	 * 
	 * @param key
	 *            String[]
	 * @throws Exception
	 * @return Map
	 */
	public Map get(String[] key) throws Exception {
		Map rtn = null;
		if (EasyMemTransactionFactory.existTransaction()) {
			rtn = EasyMemTransactionFactory.getFromCache(key);
			if (rtn == null) {
				rtn = this._get(key);
				EasyMemTransactionFactory.putIntoCache(key, rtn);
			} else {
				if (rtn.equals(EasyMemTransactionFactory.NULL_OBJECT)) {
					rtn = null;
				}
			}
		} else {
			rtn = this._get(key);
		}
		return rtn;
	}

	/**
	 * 根据key获得value
	 * 
	 * @param key
	 *            String
	 * @throws Exception
	 * @return Object
	 */
	private Map _get(String[] key) throws Exception {
		if (key == null || key.length == 0) {
			return null;
		}

		Map rtn = null;

		Socket socket = null;
		SocketObjectPool objSocketObjectPool = null;
		try {
			objSocketObjectPool = LoadBalanceFactory.getInstance().getSocketObjectPool();
			socket = (Socket) objSocketObjectPool.borrowObject();

			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			out.write(EasyMemBufferedDriver.BYTE_GET);

			for (int i = 0; i < key.length; i++) {
				if (StringUtils.isBlank(key[i])) {
					throw new Exception("获得多个值,其中有key为空的情况");
				}
				key[i] = encodeKey(key[i]);
				out.write(key[i].getBytes());
				if (i != (key.length - 1)) {
					out.write(BYTE_SPACE);
				}
			}

			out.write(EasyMemBufferedDriver.BYTE_CRLF);

			// 对于采用连接池，当easymem重启后，客户端还是保留的原来的socket的句柄
			// 当有业务访问的时候就出现了SocketFlushException和ReadNullPointException异常
			// 只需要立即删除和重建连接池
			// 如果这个时候easymem不存在了,但是客户端还是继续保留了socket句柄，会经历重试次数后，就返回失败
			try {
				out.flush();
			} catch (Exception ex) {
				throw new SocketFlushException(ex);
			}

			rtn = this.getObjectArrayFromStream(in, out);
		} catch (Exception ex) {
			if (ex instanceof ReadNullPointException) {
				String host = objSocketObjectPool.getHost();
				int port = objSocketObjectPool.getPort();
				int timeoutSeconds = objSocketObjectPool.getTimeoutSeconds();
				synchronized (this) {
					log.error("发生ReadNullPointException异常，删除连接池:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
					log.error("立即建立连接池[" + host + ":" + port + "]");
					LoadBalanceFactory.getInstance().addPool(LoadBalanceFactory.getInstance().makePool(new Server(host, port, timeoutSeconds)));
				}
			} else if (ex instanceof SocketFlushException) {
				String host = objSocketObjectPool.getHost();
				int port = objSocketObjectPool.getPort();
				int timeoutSeconds = objSocketObjectPool.getTimeoutSeconds();
				synchronized (this) {
					log.error("发生SocketFlushException异常，删除连接池:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
					log.error("立即建立连接池[" + host + ":" + port + "]");
					LoadBalanceFactory.getInstance().addPool(LoadBalanceFactory.getInstance().makePool(new Server(host, port, timeoutSeconds)));
				}
			} else {
				log.error("发生异常，删除连接池:", ex);
				LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
			}

			throw ex;
		} finally {
			if (socket != null && objSocketObjectPool != null) {
				objSocketObjectPool.returnObject(socket);
			}
		}
		return rtn;
	}

	/**
	 * 获得值为了验证
	 * 
	 * @param socket
	 *            Socket
	 * @param key
	 *            String
	 * @throws Exception
	 * @return Object
	 */
	public Object get(Socket socket, String key) throws Exception {
		Object rtn = null;

		try {
			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			key = encodeKey(key);
			out.write(EasyMemBufferedDriver.BYTE_GET);
			out.write(key.getBytes());
			out.write(EasyMemBufferedDriver.BYTE_CRLF);

			out.flush();
			rtn = getObjectFromStream(in, out);
		} catch (Exception ex) {
			throw ex;
		}
		return rtn;
	}

	/**
	 * 获得多个值
	 * 
	 * @param socket
	 *            Socket
	 * @param key
	 *            String[]
	 * @throws Exception
	 * @return Map
	 */
	public Map get(Socket socket, String[] key) throws Exception {
		if (key == null || key.length == 0) {
			return null;
		}

		Map rtn = null;
		try {
			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			out.write(EasyMemBufferedDriver.BYTE_GET);

			for (int i = 0; i < key.length; i++) {
				if (StringUtils.isBlank(key[i])) {
					throw new Exception("获得多个值,其中有key为空的情况");
				}
				key[i] = encodeKey(key[i]);
				out.write(key[i].getBytes());
				if (i != (key.length - 1)) {
					out.write(BYTE_SPACE);
				}
			}

			out.write(EasyMemBufferedDriver.BYTE_CRLF);

			out.flush();
			rtn = this.getObjectArrayFromStream(in, out);
		} catch (Exception ex) {
			throw ex;
		}
		return rtn;
	}

	/**
	 * 根据key删除value
	 * 
	 * @param key
	 *            String
	 * @throws Exception
	 * @return boolean
	 */
	public boolean delete(String key) throws Exception {
		boolean rtn = false;

		Socket socket = null;
		SocketObjectPool objSocketObjectPool = null;
		try {
			objSocketObjectPool = LoadBalanceFactory.getInstance().getSocketObjectPool();
			socket = (Socket) objSocketObjectPool.borrowObject();

			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			key = encodeKey(key);
			out.write(EasyMemBufferedDriver.BYTE_DELETE);
			out.write(key.getBytes());
			out.write(EasyMemBufferedDriver.BYTE_CRLF);
			out.flush();

			String ret = readLine(in);
			rtn = EasyMemBufferedDriver.SERVER_STATUS_DELETED.equals(ret) || EasyMemBufferedDriver.SERVER_STATUS_NOT_FOUND.equals(ret);
		} catch (Exception ex) {
			if (!(ex instanceof NormalException)) {
				log.error("发生异常，删除连接池:", ex);
				LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
			}
			throw ex;
		} finally {
			if (socket != null && objSocketObjectPool != null) {
				objSocketObjectPool.returnObject(socket);
			}
		}
		return rtn;
	}

	/**
	 * 获得状态信息
	 * 
	 * @throws Exception
	 * @return HashMap
	 */
	public HashMap stats() throws Exception {
		HashMap rtn = new HashMap();

		Socket socket = null;
		SocketObjectPool objSocketObjectPool = null;
		try {
			objSocketObjectPool = LoadBalanceFactory.getInstance().getSocketObjectPool();
			socket = (Socket) objSocketObjectPool.borrowObject();
			BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
			out.write("stats".getBytes());
			out.write(EasyMemBufferedDriver.BYTE_CRLF);

			out.flush();

			rtn.put("host", socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
			String cmd = readLine(in);
			while (!EasyMemBufferedDriver.SERVER_STATUS_END.equals(cmd)) {
				String[] yh = StringUtils.split(cmd, " ");
				rtn.put(yh[1], yh[2]);
				cmd = readLine(in);
			}
		} catch (Exception ex) {
			if (!(ex instanceof NormalException)) {
				log.error("发生异常，删除连接池:", ex);
				LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
			}
			throw ex;
		} finally {
			if (socket != null && objSocketObjectPool != null) {
				objSocketObjectPool.returnObject(socket);
			}
		}
		return rtn;
	}

	/**
	 * 根据流获得对象
	 * 
	 * @param in
	 *            InputStream
	 * @param out
	 *            OutputStream
	 * @throws ReadNullPointException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @return Object
	 */
	private Object getObjectFromStream(InputStream in, OutputStream out) throws ReadNullPointException, IOException, ClassNotFoundException {
		String cmd = readLine(in);

		// 避免再出现空指针的问题
		if (cmd == null) {
			throw new ReadNullPointException("读取命令出现返回空指针");
		}

		if (cmd.startsWith(EasyMemBufferedDriver.SERVER_STATUS_VALUE)) {
			// return object
			String[] part = StringUtils.split(cmd, " ");
			int flag = Integer.parseInt(part[2]);
			int length = Integer.parseInt(part[3]);

			byte[] bs = new byte[length];

			int count = 0;
			while (count < bs.length) {
				count += in.read(bs, count, (bs.length - count));
			}
			if (count != bs.length) {
				throw new IOException("读取数据长度错误");
			}

			readLine(in);

			String endstr = readLine(in);
			if (EasyMemBufferedDriver.SERVER_STATUS_END.equals(endstr)) {
				// 解压
				if ((flag & F_COMPRESSED) != 0) {
					GZIPInputStream gzi = new GZIPInputStream(new ByteArrayInputStream(bs));
					ByteArrayOutputStream bos = new ByteArrayOutputStream(bs.length);

					count = 0;
					byte[] tmp = new byte[2048];
					while ((count = gzi.read(tmp)) != -1) {
						bos.write(tmp, 0, count);
					}

					bs = bos.toByteArray();
					gzi.close();
				}
				// 解压结束
				return this.byte2Object(bs);
			} else {
				throw new IOException("结束标记错误");
			}
		}
		return null;
	}

	/**
	 * 根据流获得对象
	 * 
	 * @param in
	 *            InputStream
	 * @param out
	 *            OutputStream
	 * @throws ReadNullPointException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @return HashMap
	 */
	private HashMap getObjectArrayFromStream(InputStream in, OutputStream out) throws ReadNullPointException, IOException, ClassNotFoundException {

		HashMap map = new HashMap();

		while (true) {
			String cmd = readLine(in);
			// 避免再出现空指针的问题
			if (cmd == null) {
				throw new ReadNullPointException("读取命令出现返回空指针");
			}

			if (cmd.startsWith(EasyMemBufferedDriver.SERVER_STATUS_VALUE)) {
				// return object
				String[] part = StringUtils.split(cmd, " ");
				String key = part[1].trim();
				int flag = Integer.parseInt(part[2]);
				int length = Integer.parseInt(part[3]);

				byte[] bs = new byte[length];

				int count = 0;
				while (count < bs.length) {
					count += in.read(bs, count, (bs.length - count));
				}
				if (count != bs.length) {
					throw new IOException("读取数据长度错误");
				}

				// 解压
				if ((flag & F_COMPRESSED) != 0) {
					GZIPInputStream gzi = new GZIPInputStream(new ByteArrayInputStream(bs));
					ByteArrayOutputStream bos = new ByteArrayOutputStream(bs.length);

					count = 0;
					byte[] tmp = new byte[2048];
					while ((count = gzi.read(tmp)) != -1) {
						bos.write(tmp, 0, count);
					}

					bs = bos.toByteArray();
					gzi.close();
				}
				// 解压结束
				map.put(decodeKey(key), this.byte2Object(bs));

				cmd = readLine(in);
			} else if (EasyMemBufferedDriver.SERVER_STATUS_END.equals(cmd)) {
				// 读取到结束符号
				break;
			} else {
				throw new IOException("结束标记错误");
			}
		}

		return map;
	}

	/**
	 * 对key进行解码
	 * 
	 * @param key
	 *            String
	 * @throws UnsupportedEncodingException
	 * @return String
	 */
	private String decodeKey(String key) throws UnsupportedEncodingException {
		return URLDecoder.decode(key, EasyMemBufferedDriver.ENCODING_TYPE);
	}

	/**
	 * 对key进行编码
	 * 
	 * @param key
	 *            String
	 * @throws UnsupportedEncodingException
	 * @return String
	 */
	private String encodeKey(String key) throws UnsupportedEncodingException {
		return URLEncoder.encode(key, EasyMemBufferedDriver.ENCODING_TYPE);
	}

	/**
	 * 读一行数据
	 * 
	 * @param in
	 *            InputStream
	 * @throws IOException
	 * @return String
	 */
	private String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		boolean eol = false;
		byte[] b = new byte[1];
		while (in.read(b, 0, 1) != -1) {
			if (b[0] == 13) {
				eol = true;
			} else if (eol && b[0] == 10) {
				break;
			} else {
				eol = false;
			}

			bos.write(b, 0, 1);
		}

		if (bos.size() == 0) {
			return null;
		}
		return bos.toString().trim();
	}

	/**
	 * 对象转为字节
	 * 
	 * @param o
	 *            Object
	 * @throws IOException
	 * @return byte[]
	 */
	private byte[] object2Byte(Object o) throws IOException {
		return IOFactory.object2bytes(o);
	}

	/**
	 * 字节转为对象
	 * 
	 * @param b
	 *            byte[]
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @return Object
	 */
	private Object byte2Object(byte[] b) throws IOException, ClassNotFoundException {
		return IOFactory.bytes2object(b);
	}

}
