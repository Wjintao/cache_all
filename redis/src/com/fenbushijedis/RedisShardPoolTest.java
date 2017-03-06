package com.fenbushijedis;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

public class RedisShardPoolTest {

    static ShardedJedisPool pool;

    static {

        JedisPoolConfig config = new JedisPoolConfig();// Jedis池配置

        config.setMaxActive(500);// 最大活动的对象个数

        config.setMaxIdle(1000 * 60);// 对象最大空闲时间

        config.setMaxWait(1000 * 10);// 获取对象时最大等待时间

        config.setTestOnBorrow(true);

        String hostA = "192.168.128.111";

        int portA = 6379;

        String hostB = "192.168.128.123";

        int portB = 6379;

        List<JedisShardInfo> jdsInfoList = new ArrayList<JedisShardInfo>(2);

        JedisShardInfo infoA = new JedisShardInfo(hostA, portA);

        infoA.setPassword("redis.360buy");

        JedisShardInfo infoB = new JedisShardInfo(hostB, portB);

        infoB.setPassword("redis.360buy");

        jdsInfoList.add(infoA);

        jdsInfoList.add(infoB);


        pool =
                new ShardedJedisPool(config, jdsInfoList, Hashing.MURMUR_HASH,
                        Sharded.DEFAULT_KEY_TAG_PATTERN);
        // 传入连接池配置、分布式redis服务器主机信息、分片规则（存储到哪台redis服务器）
    }


    /**
     * 
     * @param args
     */

    public static void main(String[] args) {

        for (int i = 0; i < 100; i++) {
            String key = generateKey();
            // key += "{aaa}";
            ShardedJedis jds = null;
            try {
                jds = pool.getResource();
                System.out.println(key + ":" + jds.getShard(key).getClient().getHost());
                System.out.println(jds.set(key, "1111111111111111111111111111111"));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pool.returnResourceObject(jds);
            }

        }

    }


    private static int index = 1;

    public static String generateKey() {

        return String.valueOf(Thread.currentThread().getId()) + "_" + (index++);

    }
}
