package com.asiainfo.easymem.policy;

/**
 * 获得bind对象的负载均衡策略接口
 * 
 * @author linzhaoming
 * 
 *         Created at 2012-10-15
 */
public interface IPolicy {

	/** 获得对象 */
	public Object getPolicyObject() throws Exception;

	/** 增加一个对象 */
	public boolean add(Object o);

	/** 增加一个对象 */
	public boolean remove(Object o);

	/** 清除所有对象 */
	public void clear();

	/** 大小 */
	public int size();

	/** 获得所有对象 */
	public Object[] toArray();

	/** 是否包含对象 */
	public boolean contains(Object o);

}
