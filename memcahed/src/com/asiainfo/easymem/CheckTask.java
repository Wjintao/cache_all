package com.asiainfo.easymem;

import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.asiainfo.easymem.policy.LoadBalanceFactory;
import com.asiainfo.easymem.policy.Server;
import com.asiainfo.easymem.pool.SocketObjectPool;
import com.asiainfo.easymem.validate.ValidateFactory;

/**
 * 检查线程
 * 
 * @author linzhaoming
 * 
 *         Created at 2013-6-4
 */
public class CheckTask extends TimerTask {
	private transient static Log log = LogFactory.getLog(CheckTask.class);

	/**
	 * 任务运行
	 */
	public void run() {
		Thread.currentThread().setName("easymem检查线程");

		if (log.isInfoEnabled()) {
			log.info("检查线程开始工作");
		}

		try {
			Object[] ok_servers = LoadBalanceFactory.getInstance().getArivableServers();
			List all_servers = LoadBalanceFactory.getInstance().getAllServers();
			for (Iterator iter = all_servers.iterator(); iter.hasNext();) {
				Server item = (Server) iter.next();
				if (!isArivableServer(item, ok_servers)) {
					SocketObjectPool objSocketObjectPool = LoadBalanceFactory.getInstance().makePool(item);
					Socket socket = null;
					try {
						socket = (Socket) objSocketObjectPool.borrowObject();

						if (ValidateFactory.validate(socket)) {
							LoadBalanceFactory.getInstance().addPool(objSocketObjectPool);
							if (log.isInfoEnabled()) {
								log.info("检查线程发现连接池:" + objSocketObjectPool.toString() + "验证成功");
							}
						} else {
							throw new Exception("Socket:" + socket + ",验证失败");
						}
					} catch (Exception ex1) {
						if (objSocketObjectPool != null) {
							if (log.isInfoEnabled()) {
								log.info("检查线程发现连接池:" + objSocketObjectPool.toString() + "验证失败", ex1);
							}
							objSocketObjectPool.clear();
							objSocketObjectPool = null; // help gc
						}
					} finally {
						if (socket != null && objSocketObjectPool != null) {
							objSocketObjectPool.returnObject(socket);
						}
					}
				}
			}
		} catch (Throwable ex) {
			log.error("检查线程检查时候发现外层异常", ex);
		}

		if (log.isInfoEnabled()) {
			log.info("检查线程完成工作");
		}

	}

	/** 是否是已经运行的server */
	private boolean isArivableServer(Server server, Object[] objects) {
		boolean rtn = false;
		for (int i = 0; i < objects.length; i++) {
			SocketObjectPool objSocketObjectPool = (SocketObjectPool) objects[i];
			if (objSocketObjectPool.getHost().equalsIgnoreCase(server.getHost()) && objSocketObjectPool.getPort() == server.getPort()) {
				rtn = true;
				break;
			}
		}
		return rtn;
	}

	public static void main(String[] args) throws Exception {
		Timer timer = new Timer();
		timer.schedule(new CheckTask(), 10, 5);
	}
}
