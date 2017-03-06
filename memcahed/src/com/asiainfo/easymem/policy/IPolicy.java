package com.asiainfo.easymem.policy;

/**
 * ���bind����ĸ��ؾ�����Խӿ�
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public interface IPolicy {

	/** ��ö��� */
	public Object getPolicyObject() throws Exception;

	/** ����һ������ */
	public boolean add(Object o);

	/** ����һ������ */
	public boolean remove(Object o);

	/** ������ж��� */
	public void clear();

	/** ��С */
	public int size();

	/** ������ж��� */
	public Object[] toArray();

	/** �Ƿ�������� */
	public boolean contains(Object o);

}
