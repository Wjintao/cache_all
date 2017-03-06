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
 * easymem������
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class EasyMemBufferedDriver implements IEasyMemDriver {
	private transient static Log log = LogFactory.getLog(EasyMemBufferedDriver.class);

	// ѹ����־
	private static final int F_COMPRESSED = 2;

	// �ַ������ֽ�
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

	// ����ֽ���
	public static int MAX_BYTE_SIZE = 5 * 1024 * 1024;
	// ����ֽڿ�ʼѹ��
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
			log.error("���ѹ����ֵ��������ֽ�������,����Ĭ��ֵ,��Ӱ��ϵͳ����", ex);
		}
	}

	/** ����key��value�����е�server */
	public boolean setKeyAndValue2AllServer(String key, Object obj) throws Exception {
		if (obj == null || key == null || "".equals(key)) {
			throw new Exception("key��value����Ϊ��");
		}

		// �Ƿ��trace
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
		// ��trace����

		boolean rtn = false;

		// ����һ�ζ�key���б��룬������ѭ���ж�key���б��룬�����ظ�������Ӧ��key�����˱仯
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

						// ���ܵ�ַ
						if (StringUtils.isBlank(objEasyMemTrace.getHost())) {
							objEasyMemTrace.setHost(address.getHostName() + ":" + address.getPort());
						} else {
							objEasyMemTrace.setHost(objEasyMemTrace.getHost() + "," + address.getHostName() + ":" + address.getPort());
						}

						// ����ʱ��
						if (getTimeStart > 0) {
							objEasyMemTrace.setGetTime(objEasyMemTrace.getGetTime() + ((int) (System.currentTimeMillis() - getTimeStart)));
						}
					}
				}

				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
				BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

				int flag = 0;

				// ���л�����
				byte[] bs = object2Byte(obj);

				// ѹ��
				if (bs.length > COMPRESS_THRESHOLD) {
					ByteArrayOutputStream bos = new ByteArrayOutputStream(bs.length);
					GZIPOutputStream gos = new GZIPOutputStream(bos);
					gos.write(bs, 0, bs.length);
					gos.finish();

					bs = bos.toByteArray();
					flag |= F_COMPRESSED;
				}
				// ѹ������

				// ����ֽ�����
				if (bs.length >= MAX_BYTE_SIZE) {
					throw new NormalException("���ܳ���" + MAX_BYTE_SIZE + "�ֽ�");
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
					throw new Exception("set���ִ���:" + ret);
				}

				// trace
				if (objEasyMemTrace != null) {
					objEasyMemTrace.setSuccess(true);
					// ����ʱ��
					objEasyMemTrace.setUseTime(objEasyMemTrace.getUseTime() + (int) (System.currentTimeMillis() - start));
					objEasyMemTrace.setProcessMethod(EasyMemTrace.PROCESS_METHOD_SET);

					// ��������һ��
					if (i == (arivableServers.length - 1)) {
						TraceFactory.addTraceInfo(objEasyMemTrace);
					}
				}
			} catch (Exception ex) {
				if (!(ex instanceof NormalException)) {
					log.error("�����쳣��ɾ�����ӳ�:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
				}

				// ��ΪȨ��cache��set���ɹ���������ҵ������
				// ���׳��쳣������ѭ������

				// trace
				if (objEasyMemTrace != null) {
					objEasyMemTrace.setSuccess(false);
					// ����ʱ��
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
	 * ����key��value
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
			throw new Exception("key��value����Ϊ��");
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

			// ���л�����
			byte[] bs = object2Byte(obj);

			// ѹ��
			if (bs.length > COMPRESS_THRESHOLD) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream(bs.length);
				GZIPOutputStream gos = new GZIPOutputStream(bos);
				gos.write(bs, 0, bs.length);
				gos.finish();

				bs = bos.toByteArray();
				flag |= F_COMPRESSED;
			}
			// ѹ������

			// ����ֽ�����
			if (bs.length >= MAX_BYTE_SIZE) {
				throw new NormalException("���ܳ���" + MAX_BYTE_SIZE + "�ֽ�");
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
				throw new Exception("set���ִ���:" + ret);
			}
		} catch (Exception ex) {
			if (!(ex instanceof NormalException)) {
				log.error("�����쳣��ɾ�����ӳ�:", ex);
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
	 * ����key���value
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
	 * ����key���value
	 * 
	 * @param key
	 *            String
	 * @throws Exception
	 * @return Object
	 */
	private Object _get(String key) throws Exception {
		Object rtn = null;

		long start = 0;
		// �Ƿ��trace
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
		// ��trace����

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

			// ���ڲ������ӳأ���easymem�����󣬿ͻ��˻��Ǳ�����ԭ����socket�ľ��
			// ����ҵ����ʵ�ʱ��ͳ�����SocketFlushException��ReadNullPointException�쳣
			// ֻ��Ҫ����ɾ�����ؽ����ӳ�
			// ������ʱ��easymem��������,���ǿͻ��˻��Ǽ���������socket������ᾭ�����Դ����󣬾ͷ���ʧ��
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
					log.error("����ReadNullPointException�쳣��ɾ�����ӳ�:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
					log.error("�����������ӳ�[" + host + ":" + port + "]");
					LoadBalanceFactory.getInstance().addPool(LoadBalanceFactory.getInstance().makePool(new Server(host, port, timeoutSeconds)));
				}
			} else if (ex instanceof SocketFlushException) {
				String host = objSocketObjectPool.getHost();
				int port = objSocketObjectPool.getPort();
				int timeoutSeconds = objSocketObjectPool.getTimeoutSeconds();
				synchronized (this) {
					log.error("����SocketFlushException�쳣��ɾ�����ӳ�:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
					log.error("�����������ӳ�[" + host + ":" + port + "]");
					LoadBalanceFactory.getInstance().addPool(LoadBalanceFactory.getInstance().makePool(new Server(host, port, timeoutSeconds)));
				}
			} else {
				log.error("�����쳣��ɾ�����ӳ�:", ex);
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
	 * ����key���value
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
	 * ����key���value
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
					throw new Exception("��ö��ֵ,������keyΪ�յ����");
				}
				key[i] = encodeKey(key[i]);
				out.write(key[i].getBytes());
				if (i != (key.length - 1)) {
					out.write(BYTE_SPACE);
				}
			}

			out.write(EasyMemBufferedDriver.BYTE_CRLF);

			// ���ڲ������ӳأ���easymem�����󣬿ͻ��˻��Ǳ�����ԭ����socket�ľ��
			// ����ҵ����ʵ�ʱ��ͳ�����SocketFlushException��ReadNullPointException�쳣
			// ֻ��Ҫ����ɾ�����ؽ����ӳ�
			// ������ʱ��easymem��������,���ǿͻ��˻��Ǽ���������socket������ᾭ�����Դ����󣬾ͷ���ʧ��
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
					log.error("����ReadNullPointException�쳣��ɾ�����ӳ�:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
					log.error("�����������ӳ�[" + host + ":" + port + "]");
					LoadBalanceFactory.getInstance().addPool(LoadBalanceFactory.getInstance().makePool(new Server(host, port, timeoutSeconds)));
				}
			} else if (ex instanceof SocketFlushException) {
				String host = objSocketObjectPool.getHost();
				int port = objSocketObjectPool.getPort();
				int timeoutSeconds = objSocketObjectPool.getTimeoutSeconds();
				synchronized (this) {
					log.error("����SocketFlushException�쳣��ɾ�����ӳ�:", ex);
					LoadBalanceFactory.getInstance().deletePool(objSocketObjectPool);
					log.error("�����������ӳ�[" + host + ":" + port + "]");
					LoadBalanceFactory.getInstance().addPool(LoadBalanceFactory.getInstance().makePool(new Server(host, port, timeoutSeconds)));
				}
			} else {
				log.error("�����쳣��ɾ�����ӳ�:", ex);
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
	 * ���ֵΪ����֤
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
	 * ��ö��ֵ
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
					throw new Exception("��ö��ֵ,������keyΪ�յ����");
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
	 * ����keyɾ��value
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
				log.error("�����쳣��ɾ�����ӳ�:", ex);
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
	 * ���״̬��Ϣ
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
				log.error("�����쳣��ɾ�����ӳ�:", ex);
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
	 * ��������ö���
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

		// �����ٳ��ֿ�ָ�������
		if (cmd == null) {
			throw new ReadNullPointException("��ȡ������ַ��ؿ�ָ��");
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
				throw new IOException("��ȡ���ݳ��ȴ���");
			}

			readLine(in);

			String endstr = readLine(in);
			if (EasyMemBufferedDriver.SERVER_STATUS_END.equals(endstr)) {
				// ��ѹ
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
				// ��ѹ����
				return this.byte2Object(bs);
			} else {
				throw new IOException("������Ǵ���");
			}
		}
		return null;
	}

	/**
	 * ��������ö���
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
			// �����ٳ��ֿ�ָ�������
			if (cmd == null) {
				throw new ReadNullPointException("��ȡ������ַ��ؿ�ָ��");
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
					throw new IOException("��ȡ���ݳ��ȴ���");
				}

				// ��ѹ
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
				// ��ѹ����
				map.put(decodeKey(key), this.byte2Object(bs));

				cmd = readLine(in);
			} else if (EasyMemBufferedDriver.SERVER_STATUS_END.equals(cmd)) {
				// ��ȡ����������
				break;
			} else {
				throw new IOException("������Ǵ���");
			}
		}

		return map;
	}

	/**
	 * ��key���н���
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
	 * ��key���б���
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
	 * ��һ������
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
	 * ����תΪ�ֽ�
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
	 * �ֽ�תΪ����
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
