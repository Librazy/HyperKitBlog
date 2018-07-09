package org.librazy.demo.dubbo.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.lambdaworks.redis.SetArgs;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.pubsub.RedisPubSubAdapter;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import org.librazy.demo.dubbo.config.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Set;

import static org.librazy.demo.dubbo.config.RedisUtils.OK;

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
                    String id = sections[2];
                    String sid = sections[4];
                    connection.sync().srem(RedisUtils.sessions(id), sid);
                }
            }
        });
        connectionPubSub.async().psubscribe("__key*__:user:*:session:*:key");
    }

    @Override
    public void newSession(String id, String sid, String ua, String key) {
        if (OK.equals(connection.sync().set(RedisUtils.key(id, sid), key, SetArgs.Builder.ex(86400L * 7).nx()))) {
            connection.sync().sadd(RedisUtils.sessions(id), sid);
            connection.sync().setex(RedisUtils.userAgent(id, sid), 86400L * 7, ua);
        } else {
            throw new IllegalStateException("Session already exist or failed to create session");
        }
    }

    @Override
    public String getKey(String id, String sid) {
        return connection.sync().get(RedisUtils.key(id, sid));
    }

    @Override
    public String getUserAgent(String id, String sid) {
        return connection.sync().get(RedisUtils.userAgent(id, sid));
    }

    @Override
    public Set<String> getSessions(String id) {
        return connection.sync().smembers(RedisUtils.sessions(id));
    }

    @Override
    public synchronized void deleteSession(String id, String sid) {
        connection.sync().multi();
        connection.sync().del(
                RedisUtils.key(id, sid),
                RedisUtils.userAgent(id, sid)
        );
        connection.sync().srem(RedisUtils.sessions(id), sid);
        connection.sync().exec();
    }

    @Override
    public synchronized void refreshSession(String id, String sid) {
        connection.sync().multi();
        connection.sync().expire(RedisUtils.key(id, sid), 86400L * 7);
        connection.sync().expire(RedisUtils.userAgent(id, sid), 86400L * 7);
        connection.sync().exec();
    }

    @Override
    public synchronized void clearSession(String id) {
        getSessions(id).forEach(sid -> deleteSession(id, sid));
    }

    @Override
    public void renameId(String id, String sid, String now) {
        if (!connection.sync().renamenx(RedisUtils.sessions(id), RedisUtils.sessions(now))) {
            throw new IllegalStateException("session id " + id + " already exist");
        }
        connection.sync().renamenx(RedisUtils.key(id, sid), RedisUtils.key(now, sid));
        connection.sync().renamenx(RedisUtils.userAgent(id, sid), RedisUtils.userAgent(now, sid));
    }

    @Override
    public boolean validNonce(String nonce) {
        return connection.async().set(RedisUtils.nonce(nonce), "", new SetArgs().nx().ex(20)).toCompletableFuture().thenApply(
                OK::equals
        ).join();
    }


    @Override
    public String sendCode(String email) {
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email not vaild");
        }
        String code = String.valueOf((new SecureRandom().nextInt(900000) + 100000));
        if (OK.equals(connection.sync().set(RedisUtils.codeExp(email), String.valueOf(new Date().getTime()), new SetArgs().nx().ex(60)))) {
            connection.sync().setex(RedisUtils.code(email), 300, code);
            return code;
        }
        return null;
    }

    @Override
    public boolean checkCode(String email, String req) {
        String code = connection.sync().get(RedisUtils.code(email));
        return code != null && code.equals(req);
    }
}
