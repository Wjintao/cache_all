package com.asiainfo.easyframe.redis.proxy;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

public class PipelineCache {

	private static Log log = LogFactory.getLog(PipelineCache.class);
	private String node = "";
	private Jedis jedis = null;
	private Pipeline pipeline = null;
	private String namespace = "";

	public PipelineCache(String node, Jedis jedis,String namespace) {
		this.node = node;
		this.jedis = jedis;
		this.pipeline = jedis.pipelined();
		this.namespace = namespace;
	}

	public Response<Long> incr(String key) {
		try {
			return pipeline.incr(namespace + key);
		} catch (Exception e) {
			log.error("redis pipeline incr() error ", e);
			return null;
		}
	}

	public Response<String> get(String key) {
		try {
			return pipeline.get(namespace + key);
		} catch (Exception e) {
			log.error("redis pipeline get() error ", e);
			return null;
		}
	}

	public Response<String> set(String key, String value) {
		try {
			return pipeline.set(namespace + key, value);
		} catch (Exception e) {
			log.error("redis pipeline set() error ", e);
			return null;
		}
	}

	public Response<Long> expire(String key, int seconds) {
		try {
			return pipeline.expire(namespace + key, seconds);
		} catch (Exception e) {
			log.error("redis pipeline expire() error ", e);
			return null;
		}
	}

	public Response<Long> rpush(String key, String value) {
		try {
			return pipeline.rpush(namespace + key, value);
		} catch (Exception e) {
			log.error("redis pipeline rpush() error ", e);
			return null;
		}
	}

	public Response<List<String>> lrange(String key, long start, long end) {
		try {
			return pipeline.lrange(namespace + key, start, end);
		} catch (Exception e) {
			log.error("redis pipeline lrange() error ", e);
			return null;
		}
	}

	public Response<Long> del(String key) {
		try {
			return pipeline.del(namespace + key);
		} catch (Exception e) {
			log.error("redis pipeline del() error ", e);
			return null;
		}
	}

	public void sync() {
		try {
			pipeline.sync();
		} catch (Exception e) {
			log.error("redis pipeline sync() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
		} finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}

	public List<Object> syncAndReturnAll() {
		try {
			return pipeline.syncAndReturnAll();
		} catch (Exception e) {
			log.error("redis pipeline syncAndReturnAll() error ", e);
			MyJedisPool.returnBrokenResource(node, jedis);
			return null;
		} finally {
			MyJedisPool.returnResource(node, jedis);
		}
	}
}
