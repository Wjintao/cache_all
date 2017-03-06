package com.asiainfo.easymem.driver.listener;

import java.util.HashMap;

import com.ai.appframe2.complex.transaction.listener.ITransactionListener;

/**
 * 在一个事务内部的多次easymem查询结果缓存
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class EasyMemTransactionListenerImpl implements ITransactionListener {

	public EasyMemTransactionListenerImpl() {
	}

	/** 开始事务事件 */
	public void onStartTransaction() {
		clearCache();
		EasyMemTransactionFactory.CACHE.set(new HashMap());
	}

	/** 回滚事务事件 */
	public void onRollbackTransaction() {
	}

	/** 提交事务事件 */
	public void onCommitTransaction() {
	}

	/** 完成事务事件 */
	public void onCompleteTransaction() {
		clearCache();
	}

	/** 清除cache */
	private void clearCache() {
		HashMap map = (HashMap) EasyMemTransactionFactory.CACHE.get();
		if (map != null) {
			map.clear();
			map = null; // help gc
		}
		EasyMemTransactionFactory.CACHE.set(null);
	}
}
