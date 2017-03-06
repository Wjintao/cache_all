package com.asiainfo.easyframe.redis;

import com.asiainfo.easyframe.redis.proxy.JedisCache;

public class RedisClient {

	public static JedisCache getJedis(String node) {
		return new JedisCache(node);
	}

	public static void main(String[] args) {
		for (Integer i = 0; i < 100000000; i++) {
			try {
				JedisCache jedis = getJedis("node1");
				jedis.set(i.toString(), "wugl");
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
