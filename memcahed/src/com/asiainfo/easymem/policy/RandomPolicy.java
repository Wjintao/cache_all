package com.asiainfo.easymem.policy;

import java.util.LinkedList;

/**
 * �������
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class RandomPolicy extends LinkedList implements IPolicy {
	public RandomPolicy() {
	}

	public Object getPolicyObject() throws Exception {
		return super.get(getRandom(0, super.size() - 1));
	}

	public Object[] toArrsy() {
		return super.toArray();
	}

	public void clear() {
		super.clear();
	}

	/** �����С�����֮�������� */
	private int getRandom(int min, int max) {
		return min + (int) (Math.random() * (max - min + 1));
	}

}
