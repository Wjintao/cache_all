package com.asiainfo.easymem.driver.listener;

import java.util.HashMap;

import com.ai.appframe2.complex.transaction.listener.ITransactionListener;

/**
 * ��һ�������ڲ��Ķ��easymem��ѯ�������
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class EasyMemTransactionListenerImpl implements ITransactionListener {

	public EasyMemTransactionListenerImpl() {
	}

	/** ��ʼ�����¼� */
	public void onStartTransaction() {
		clearCache();
		EasyMemTransactionFactory.CACHE.set(new HashMap());
	}

	/** �ع������¼� */
	public void onRollbackTransaction() {
	}

	/** �ύ�����¼� */
	public void onCommitTransaction() {
	}

	/** ��������¼� */
	public void onCompleteTransaction() {
		clearCache();
	}

	/** ���cache */
	private void clearCache() {
		HashMap map = (HashMap) EasyMemTransactionFactory.CACHE.get();
		if (map != null) {
			map.clear();
			map = null; // help gc
		}
		EasyMemTransactionFactory.CACHE.set(null);
	}
}
