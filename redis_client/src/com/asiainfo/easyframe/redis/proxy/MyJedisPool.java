package com.asiainfo.easyframe.redis.proxy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

import com.asiainfo.easyframe.redis.common.Constant;

public class MyJedisPool {

	private static Log log = LogFactory.getLog(MyJedisPool.class);

	private static Map<String, JedisSentinelPool> jedisPoolMap = new HashMap<String, JedisSentinelPool>();

	private MyJedisPool() {
	}

	static Jedis getResource(String node) {
		JedisSentinelPool jedisPool = null;
		Jedis resource = null;
		try {
			jedisPool = getJedisPool(node);
			resource = jedisPool.getResource();
		} catch (Exception e) {
			log.error("redis getResource() error", e);
			if (jedisPool != null) {
				jedisPool.destroy();
				jedisPoolMap.remove(node);
			}
		}
		return resource;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static JedisSentinelPool getJedisPool(String node) {
		JedisSentinelPool jedisPool = jedisPoolMap.get(node);
		try {
			if (jedisPool == null) {

				GenericObjectPoolConfig config = new GenericObjectPoolConfig();
				config.setMaxTotal(Integer.parseInt(Constant.getProperty("redis.pool.maxActive")));
				config.setMaxIdle(Integer.parseInt(Constant.getProperty("redis.pool.maxIdle")));
				config.setMaxWaitMillis(Integer.parseInt(Constant.getProperty("redis.pool.maxWait")));
				config.setTestOnBorrow(true);
				config.setTestOnReturn(true);

				String[] ips = Constant.getProperty("redis.ip." + node).split(",");
				String[] ports = Constant.getProperty("redis.port." + node).split(",");

				Set sentinels = new HashSet();
				for (int i = 0; i < ips.length; i++) {
					sentinels.add(new HostAndPort(ips[i], Integer.parseInt(ports[i])).toString());
				}

				jedisPool = new JedisSentinelPool("mymaster", sentinels, config, 60000);
				jedisPoolMap.put(node, jedisPool);
			}
		} catch (Exception e) {
			log.error("redis getJedisPool() error", e);
			if (jedisPool != null) {
				jedisPool.destroy();
				jedisPoolMap.remove(node);
			}
		}
		return jedisPool;
	}

	static void returnResource(String node, Jedis resource) {
		JedisSentinelPool jedisPool = jedisPoolMap.get(node);
		try {
			if (jedisPool != null && resource != null) {
				jedisPool.returnResource(resource);
			}
		} catch (Exception e) {
			log.error("redis returnResource() error", e);
			if (jedisPool != null) {
				jedisPool.destroy();
				jedisPoolMap.remove(node);
			}
		}
	}

	static void returnBrokenResource(String node, Jedis resource) {
		JedisSentinelPool jedisPool = jedisPoolMap.get(node);
		try {
			if (jedisPool != null && resource != null) {
				jedisPool.returnBrokenResource(resource);
			}
		} catch (Exception e) {
			log.error("redis returnBrokenResource() error", e);
			if (jedisPool != null) {
				jedisPool.destroy();
				jedisPoolMap.remove(node);
			}
		}
	}
}
