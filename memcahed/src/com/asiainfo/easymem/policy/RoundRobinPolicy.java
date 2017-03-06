package com.asiainfo.easymem.policy;

import java.util.LinkedList;

/**
 * round-robin����
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public class RoundRobinPolicy extends LinkedList implements IPolicy {
	private int position;

	public RoundRobinPolicy() {
	}

	public synchronized Object getPolicyObject() throws Exception {
		if (position >= (super.size() - 1)) {
			position = 0;
		} else {
			position++;
		}

		return super.get(position);
	}

	/** ��� */
	public void clear() {
		super.clear();
	}
}
