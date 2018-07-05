package org.librazy.demo.dubbo.config;

import java.text.MessageFormat;

public class RedisUtils {

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

    public static String SignupSession(String id) {
        return MessageFormat.format("user:{0}:{1}", id, SRP_SIGNUP_SESSION);
    }

    public static String SrpSession(String id) {
        return MessageFormat.format("user:{0}:{1}", id, SRP_SESSION);
    }

    public static String Sessions(String id) {
        return MessageFormat.format("user:{0}:{1}", id, USER_SESSIONS);
    }

    public static String Key(String id, String sid) {
        return MessageFormat.format("user:{0}:session:{1}:{2}", id, sid, USER_SESSION_KEY);
    }

    public static String UserAgent(String id, String sid) {
        return MessageFormat.format("user:{0}:session:{1}:{2}", id, sid, USER_SESSION_UA);
    }

    public static String Nonce(String nonce) {
        return MessageFormat.format("global:nonce:{0}", nonce);
    }

    public static String CodeExp(String email) {
        return MessageFormat.format("global:code:{0}:exp", email);
    }

    public static String Code(String email) {
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
