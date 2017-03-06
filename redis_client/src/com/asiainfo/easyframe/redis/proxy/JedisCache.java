package com.asiainfo.easyframe.redis.proxy;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;

import com.asiainfo.easyframe.redis.common.Constant;

public class JedisCache {

	private static Log log = LogFactory.getLog(JedisCache.class);
	private String node = "";
	private Jedis jedis = null;
	private String namespace = "";
	private String databases = "0";

	public JedisCache(String node) {
		this.node = node;
		jedis = MyJedisPool.getResource(node);
		namespace = Constant.getProperty("redis.namespace." + node);
		if(StringUtils.isNotBlank(namespace)){
			namespace = namespace + ":";
		}
		databases = Constant.getProperty("redis.databases." + node);
		if (StringUtils.isNotEmpty(databases) && !databases.equals("0")) {
			jedis.select(Integer.parseInt(databases));
		}
	}

	public String get(String key) {
		try {
			return jedis.get(namespace + key);
		} catch (Exception e) {
			log.error("redis get() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		} finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}

	public String set(String key, String value) {
		try {
			return jedis.set(namespace + key, value);
		} catch (Exception e) {
			log.error("redis set() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		} finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}

	public String setex(String key, int seconds, String value) {
		try {
			return jedis.setex(namespace + key, seconds, value);
		} catch (Exception e) {
			log.error("redis setex() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		} finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}

	public Long incr(String key) {
		try {
			return jedis.incr(namespace + key);
		} catch (Exception e) {
			log.error("redis incr() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		} finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}

	public PipelineCache pipelined() {
		try {
			return new PipelineCache(node, jedis,namespace);
		} catch (Exception e) {
			log.error("redis pipelined() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		}
	}
	
	public Set keys(String pattern){
		try{
			Set set = jedis.keys(namespace + pattern);
			Set retSet = new HashSet();
			//返回的key需要去掉
			if(set!=null && set.size()>0){
				Iterator<String> iter = set.iterator();
				while(iter.hasNext()){
					String key = iter.next();
					String retKey = StringUtils.substringAfter(key, namespace);
					retSet.add(retKey);
				}
			}
			return retSet;
		}catch (Exception e) {
			log.error("redis pipelined() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		}finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}
	
	public Long del(String key){
		try{
			return jedis.del(namespace + key);
		}catch (Exception e) {
			log.error("redis del() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		}finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}
	
	public Long expire(String key,int seconds){
		try{
			return jedis.expire(namespace + key, seconds);
		}catch (Exception e) {
			log.error("redis expire() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		}finally {
			MyJedisPool.returnResource(node, jedis);
		}
	} 
	
	
	public Long hincrBy(String key, String field, long value) {
		try {
			return jedis.hincrBy(namespace + key,field,value);
		} catch (Exception e) {
			log.error("redis hincrBy() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		} finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}
}
