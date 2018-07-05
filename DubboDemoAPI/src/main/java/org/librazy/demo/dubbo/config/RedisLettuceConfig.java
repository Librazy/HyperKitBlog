package org.librazy.demo.dubbo.config;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DefaultClientResources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import redis.embedded.RedisServer;

@Configuration
public class RedisLettuceConfig {

    @Value("${redis.host}")
    protected String host;

    @Value("${redis.port}")
    protected int port;

    @Value("${redis.password}")
    protected String password;


    @Profile({"default"})
    @SuppressWarnings("SameReturnValue")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Bean()
    RedisServer redisServerNop() {
        return null;
    }

    @Profile({"dev", "test-default"})
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Bean(destroyMethod = "stop")
    RedisServer redisServer() {
        RedisServer redisServer = RedisServer.builder().port(port).setting("maxheap 128M").setting("bind 127.0.0.1").build();
        try {
            redisServer.start();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return redisServer;
    }

    @Bean(destroyMethod = "shutdown")
    ClientResources clientResources(RedisServer redisServer) {
        redisServer.isActive();
        return DefaultClientResources.create();
    }

    @Bean(destroyMethod = "shutdown")
    RedisClient redisClient(ClientResources clientResources) {
        return RedisClient.create(clientResources, RedisURI.builder().withHost(host).withPort(port).withPassword(password).build());
    }

    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, String> connection(RedisClient redisClient) {
        return redisClient.connect();
    }

    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Bean(destroyMethod = "close")
    StatefulRedisPubSubConnection<String, String> connectionPubSub(RedisClient redisClient) {
        return redisClient.connectPubSub();
    }
}