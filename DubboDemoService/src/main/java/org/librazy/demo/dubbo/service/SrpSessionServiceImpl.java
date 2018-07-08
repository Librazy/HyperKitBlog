package org.librazy.demo.dubbo.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.bitbucket.thinbus.srp6.js.SRP6JavascriptServerSession;
import com.bitbucket.thinbus.srp6.js.SRP6JavascriptServerSessionSHA256;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdaworks.redis.SetArgs;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import org.librazy.demo.dubbo.config.RedisUtils;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Base64;

import static org.librazy.demo.dubbo.config.RedisUtils.OK;

@SuppressWarnings("unchecked")
@Service
@Component
public class SrpSessionServiceImpl implements SrpSessionService {

    private static Logger logger = LoggerFactory.getLogger(SrpSessionServiceImpl.class);

    private final ObjectMapper mapper;

    private final StatefulRedisConnection<String, String> connection;

    private final UserSessionService userSessionService;

    private SRP6JavascriptServerSession session;

    private Long id;

    @Autowired
    public SrpSessionServiceImpl(ObjectMapper mapper, StatefulRedisConnection<String, String> connection, UserSessionService userSessionService) {
        this.mapper = mapper;
        this.connection = connection;
        this.userSessionService = userSessionService;
    }

    @Override
    public void newSession(String n, String g) {
        session = new SRP6JavascriptServerSessionSHA256(n, g);
    }

    @Override
    public void loadSession(long id) {
        this.id = id;
        String b64os = connection.sync().get(RedisUtils.srpSession(String.valueOf(id)));
        if (b64os == null) throw new IllegalStateException();
        try {
            byte[] bs = Base64.getDecoder().decode(b64os);
            ByteArrayInputStream bais = new ByteArrayInputStream(bs);
            ObjectInputStream ois = new ObjectInputStream(bais);
            session = (SRP6JavascriptServerSession) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String step1(SrpAccountEntity account) {
        id = account.getId();
        String b = session.step1(account.getUser().getEmail(), account.getSalt(),
                account.getVerifier());
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(session);
            String b64os = Base64.getEncoder().encodeToString(baos.toByteArray());
            String result = connection.sync().set(RedisUtils.srpSession(String.valueOf(id)), b64os, SetArgs.Builder.ex(30).nx());
            if (!result.equals(OK)) {
                throw new IllegalStateException("session already exists");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return b;
    }

    @Override
    public String step2(String a, String m1, String ua) {
        try {
            String m2 = session.step2(a, m1);
            String sid = session.getSessionKey(true);
            String key = session.getSessionKey(false);
            userSessionService.newSession(String.valueOf(id), sid, ua, key);
            return m2;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        } finally {
            connection.sync().del(RedisUtils.srpSession(String.valueOf(id)));
        }
    }

    @Override
    public String getSessionKey(boolean doHash) {
        if (session == null) {
            throw new UnsupportedOperationException("session not created/restored yet");
        }
        return session.getSessionKey(doHash);
    }

    @Override
    public void saveSignup(SrpSignupForm signupForm) {
        if (id >= 0) throw new IllegalArgumentException("Trying to run saveSignup with positive id:" + id);
        try {
            String set = connection.sync().set(RedisUtils.signupSession(String.valueOf(id)), mapper.writeValueAsString(signupForm), SetArgs.Builder.ex(30).nx());
            logger.info("save signup of {}: {}", signupForm.getEmail(), set);
            if (!OK.equals(set)) {
                throw new IllegalStateException("signup form already exists");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public SrpSignupForm getSignup() {
        if (id >= 0) throw new IllegalStateException("Trying to run getSignup with positive id:" + id);
        try {
            String json = connection.sync().get(RedisUtils.signupSession(String.valueOf(id)));
            return mapper.readerFor(SrpSignupForm.class).readValue(json);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void confirmSignup(long now) {
        String sid = session.getSessionKey(true);
        if (id >= 0) throw new IllegalStateException("Trying to run confirmSignup with positive id:" + id);
        if (!userSessionService.renameId(String.valueOf(id), sid, String.valueOf(now)))
            throw new IllegalStateException("id already exists:" + now);
    }
}
