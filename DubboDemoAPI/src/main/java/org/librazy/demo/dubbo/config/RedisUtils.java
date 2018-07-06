package org.librazy.demo.dubbo.config;

import java.text.MessageFormat;

public class RedisUtils {

    public static final String OK = "OK";

    // SRP Login Session, Base64 binary of SRP6JavascriptServerSessionSHA256
    private static final String SRP_SESSION = "srp"; // user:<id>:srp

    // SRP Sign-up Session, Json of SrpSignupForm
    private static final String SRP_SIGNUP_SESSION = "signup"; // user:<id>:signup

    // SRP Session Names, Set<String>
    private static final String USER_SESSIONS = "sessions"; // user:<id>:sessions

    // SRP Session Key, hex
    private static final String USER_SESSION_KEY = "key"; // user:<id>:session:<sid>:key

    // SRP Session UserAgent, String
    private static final String USER_SESSION_UA = "ua"; // user:<id>:session:<sid>:ua
    private static final String USER_RECORDS = "user:{0}:{1}";
    private static final String USER_SESSION_RECORDS = "user:{0}:session:{1}:{2}";

    private RedisUtils() {
        throw new UnsupportedOperationException();
    }

    public static String signupSession(String id) {
        return MessageFormat.format(USER_RECORDS, id, SRP_SIGNUP_SESSION);
    }

    public static String srpSession(String id) {
        return MessageFormat.format(USER_RECORDS, id, SRP_SESSION);
    }

    public static String sessions(String id) {
        return MessageFormat.format(USER_RECORDS, id, USER_SESSIONS);
    }

    public static String key(String id, String sid) {
        return MessageFormat.format(USER_SESSION_RECORDS, id, sid, USER_SESSION_KEY);
    }

    public static String userAgent(String id, String sid) {
        return MessageFormat.format(USER_SESSION_RECORDS, id, sid, USER_SESSION_UA);
    }

    public static String nonce(String nonce) {
        return MessageFormat.format("global:nonce:{0}", nonce);
    }

    public static String codeExp(String email) {
        return MessageFormat.format("global:code:{0}:exp", email);
    }

    public static String code(String email) {
        return MessageFormat.format("global:code:{0}", email);
    }
    /*                     <users>
     *                     /     \
     *                    id      id
     *                    |        \      \
     *                <sessions>    srp    signup
     *                /        \
     *               sid       sid
     *              /   \
     *            key    ua
     *
     */
}
