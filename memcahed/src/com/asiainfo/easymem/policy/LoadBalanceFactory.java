package com.asiainfo.easymem.policy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.EasyMemConfigure;
import com.asiainfo.easymem.pool.SocketObjectPool;
import com.asiainfo.easymem.pool.SocketPoolableObjectFactory;

public class LoadBalanceFactory {
	private transient static Log log = LogFactory.getLog(LoadBalanceFactory.class);

	private static LoadBalanceFactory instance = null;

	private IPolicy objIPolicy = null;

	private List servers = new ArrayList();

	private static Boolean isInit = Boolean.FALSE;

	private String serverName = null;

	public static LoadBalanceFactory getInstance() throws Exception {
		if (instance == null) {
			synchronized (isInit) {
				if (isInit.equals(Boolean.FALSE)) {
					instance = new LoadBalanceFactory();
					isInit = Boolean.TRUE;
				}
			}
		}
		return instance;
	}

	private LoadBalanceFactory() throws Exception {
		synchronized (this) {
			try {
				objIPolicy = (IPolicy) Class.forName(EasyMemConfigure.getProperties().getProperty("server.policy")).newInstance();
			} catch (Exception ex) {
				log.error("ת������,ȡĬ������", ex);
				objIPolicy = new RoundRobinPolicy();
			}

			// Ĭ��30�볬ʱ
			int timeoutSeconds = 30;
			String strTimeoutSeconds = EasyMemConfigure.getProperties().getProperty("server.conn.timeout");
			if (!StringUtils.isBlank(strTimeoutSeconds) && StringUtils.isNumeric(strTimeoutSeconds)) {
				timeoutSeconds = Integer.parseInt(strTimeoutSeconds);
			}

			String list = EasyMemConfigure.getProperties().getProperty("server.list");
			String[] tmp = list.split(",");
			for (int i = 0; i < tmp.length; i++) {
				String[] tmp2 = tmp[i].split(":");
				Server server = new Server(tmp2[0], Integer.parseInt(tmp2[1]), timeoutSeconds);
				servers.add(server);
				addPool(makePool(server));
			}

			serverName = EasyMemConfigure.getProperties().getProperty("server.name");
			if (StringUtils.isBlank(serverName)) {
				serverName = "";
			}
		}
	}

	public SocketObjectPool makePool(Server server) throws Exception {
		SocketPoolableObjectFactory factory = new SocketPoolableObjectFactory(server.getHost(), server.getPort(), server.getTimeoutSeconds());
		SocketObjectPool pool = new SocketObjectPool(factory);
		return pool;
	}

	/** ����pool */
	public SocketObjectPool getSocketObjectPool() throws Exception {
		if (objIPolicy.size() == 0) {
			throw new Exception(serverName + "���⹤�����Ѿ�û�п�ʹ�õ�pool��");
		}
		SocketObjectPool rtn = (SocketObjectPool) objIPolicy.getPolicyObject();
		if (rtn == null) {
			throw new Exception(serverName + "���⹤�����Ѿ�û�п�ʹ�õ�pool��");
		}
		return rtn;
	}

	/** ָ�����pool���������⣬��Ҫ�Ӹ��ؾ��⹤������� */
	public void deletePool(SocketObjectPool objSocketObjectPool) {
		synchronized (objIPolicy) {
			if (objIPolicy.contains(objSocketObjectPool)) {
				if (log.isErrorEnabled()) {
					log.error("ɾ�����ӳ�:" + objSocketObjectPool);
				}

				objIPolicy.remove(objSocketObjectPool);
				objSocketObjectPool.clear();
				objSocketObjectPool = null;// help gc
			}
		}
	}

	public void addPool(SocketObjectPool objSocketObjectPool) {
		synchronized (objIPolicy) {
			objIPolicy.add(objSocketObjectPool);
		}
	}

	public List getAllServers() {
		return servers;
	}

	public Object[] getArivableServers() {
		return objIPolicy.toArray();
	}
}
