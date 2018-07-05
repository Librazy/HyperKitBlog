package org.librazy.demo.dubbo.service;

import java.io.Closeable;
import java.util.Set;

public interface UserSessionService extends Closeable {
    void newSession(String id, String sid, String ua, String key);

    String getKey(String id, String sid);

    String getUserAgent(String id, String sid);

    Set<String> getSessions(String id);

    void deleteSession(String id, String sid);

    void refreshSession(String id, String sid);

    void clearSession(String id);

    boolean renameId(String id, String sid, String now);

    boolean validNonce(String nonce);

    String sendCode(String email);

    boolean checkCode(String email, String req);

    @Override
    void close();
}
