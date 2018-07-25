package org.librazy.demo.dubbo.config;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import redis.embedded.RedisServer;

/**
 * Redis 客户端设置
 */
@Configuration
public class RedisLettuceConfig {

    private static Logger logger = LoggerFactory.getLogger(RedisLettuceConfig.class);

    /**
     * 主机
     */
    @Value("${redis.host}")
    protected String host;

    /**
     * 端口
     */
    @Value("${redis.port}")
    protected int port;

    /**
     * 密码
     */
    @Value("${redis.password}")
    protected String password;

    /**
     * 启动内置 Redis 服务器
     *
     * @return Redis 服务器
     */
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Bean(destroyMethod = "stop")
    public RedisServer redisServer() {
        RedisServer redisServer = RedisServer.builder().port(port).setting("maxmemory 128M").setting("bind 127.0.0.1").build();
        logger.info("embedded redis server starting");
        try {
            redisServer.start();
            logger.info("embedded redis server started");
        } catch (RuntimeException e) {
            logger.warn("embedded redis server failed to start", e);
        }
        return redisServer;
    }

    /**
     * 客户端配置
     *
     * @param redisServer Redis 服务器
     * @return 客户端配置
     */
    @Bean(destroyMethod = "shutdown")
    ClientResources clientResources(RedisServer redisServer) {
        logger.info("embedded redis server status {}", redisServer.isActive());
        return DefaultClientResources.create();
    }

    /**
     * Redis 客户端
     *
     * @param clientResources 客户端配置
     * @return Redis 客户端
     */
    @Bean(destroyMethod = "shutdown")
    RedisClient redisClient(ClientResources clientResources) {
        return RedisClient.create(clientResources, RedisURI.builder().withHost(host).withPort(port).withPassword(password).build());
    }

    /**
     * 有状态 Redis 连接
     *
     * @param redisClient Redis 客户端
     * @return 有状态 Redis 连接
     */
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, String> connection(RedisClient redisClient) {
        return redisClient.connect();
    }

    /**
     * 有状态 Redis 订阅
     *
     * @param redisClient Redis 客户端
     * @return 有状态 Redis 订阅
     */
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Bean(destroyMethod = "close")
    StatefulRedisPubSubConnection<String, String> connectionPubSub(RedisClient redisClient) {
        return redisClient.connectPubSub();
    }
}