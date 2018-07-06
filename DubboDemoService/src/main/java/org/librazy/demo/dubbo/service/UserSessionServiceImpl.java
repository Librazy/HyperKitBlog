package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.config.RedisUtils;
import com.alibaba.dubbo.config.annotation.Service;
import com.lambdaworks.redis.SetArgs;
import com.lambdaworks.redis.TransactionResult;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.pubsub.RedisPubSubAdapter;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Set;

@Service
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class UserSessionServiceImpl implements UserSessionService {

    private final StatefulRedisPubSubConnection<String, String> connectionPubSub;

    private final StatefulRedisConnection<String, String> connection;

    @Autowired
    public UserSessionServiceImpl(StatefulRedisPubSubConnection<String, String> connectionPubSub, StatefulRedisConnection<String, String> connection) {
        this.connectionPubSub = connectionPubSub;
        this.connection = connection;
        subscribeSessionExpired();
    }

    private void subscribeSessionExpired() {
        connectionPubSub.async().configSet("notify-keyspace-events", "xK");
        connectionPubSub.async().addListener(new RedisPubSubAdapter<String, String>() {
            @Override
            public void message(String pattern, String channel, String message) {
                if (message.equals("expired")) {
                    String[] sections = channel.split(":");
                    if (sections.length != 6) {
                        throw new RuntimeException();
                    }
                    String id = sections[2];
                    String sid = sections[4];
                    connection.sync().srem(RedisUtils.Sessions(id), sid);
                }
            }
        });
        connectionPubSub.async().psubscribe("__key*__:user:*:session:*:key");
    }

    @Override
    public void newSession(String id, String sid, String ua, String key) {
        if(!"OK".equals(connection.sync().set(RedisUtils.Key(id, sid), key, SetArgs.Builder.ex(86400 * 7).nx()))){
            connection.sync().discard();
        } else {
            connection.sync().multi();
            connection.sync().sadd(RedisUtils.Sessions(id), sid);
            connection.sync().setex(RedisUtils.UserAgent(id, sid), 86400 * 7, ua);
            TransactionResult result = connection.sync().exec();
            if (!result.wasRolledBack()) {
                return;
            }
        }
        throw new RuntimeException("Session already exist or failed to create session");
    }

    @Override
    public String getKey(String id, String sid) {
        return connection.sync().get(RedisUtils.Key(id, sid));
    }

    @Override
    public String getUserAgent(String id, String sid) {
        return connection.sync().get(RedisUtils.UserAgent(id, sid));
    }

    @Override
    public Set<String> getSessions(String id) {
        return connection.sync().smembers(RedisUtils.Sessions(id));
    }

    @Override
    public synchronized void deleteSession(String id, String sid) {
        connection.sync().multi();
        connection.sync().del(
                RedisUtils.Key(id, sid),
                RedisUtils.UserAgent(id, sid)
        );
        connection.sync().srem(RedisUtils.Sessions(id), sid);
        connection.sync().exec();
    }

    @Override
    public synchronized void refreshSession(String id, String sid) {
        connection.sync().multi();
        connection.sync().expire(RedisUtils.Key(id, sid), 86400 * 7);
        connection.sync().expire(RedisUtils.UserAgent(id, sid), 86400 * 7);
        connection.sync().exec();
    }

    @Override
    public synchronized void clearSession(String id) {
        getSessions(id).forEach(sid -> deleteSession(id, sid));
    }

    @Override
    public boolean renameId(String id, String sid, String now) {
        return connection.sync().renamenx(RedisUtils.Key(id, sid), RedisUtils.Key(now, sid))
                       && connection.sync().renamenx(RedisUtils.UserAgent(id, sid), RedisUtils.UserAgent(now, sid))
                       && connection.sync().renamenx(RedisUtils.Sessions(id), RedisUtils.Sessions(now));

    }

    @Override
    public boolean validNonce(String nonce) {
        return connection.async().set(RedisUtils.Nonce(nonce), "", new SetArgs().nx().ex(20)).toCompletableFuture().thenApply(
                result -> result.equals("OK")
        ).join();
    }


    @Override
    public String sendCode(String email) {
        if(!email.contains("@")){
            throw new RuntimeException("Email not vaild");
        }
        String code = String.valueOf((new SecureRandom().nextInt(900000) + 100000));
        if ("OK".equals(connection.sync().set(RedisUtils.CodeExp(email), String.valueOf(new Date().getTime()), new SetArgs().nx().ex(60)))) {
            connection.sync().setex(RedisUtils.Code(email), 300, code);
            return code;
        }
        return null;
    }

    @Override
    public boolean checkCode(String email, String req) {
        String code = connection.sync().get(RedisUtils.Code(email));
        return code != null && code.equals(req);
    }

    @Override
    public void close() {
        connection.close();
        connectionPubSub.close();
    }
}
