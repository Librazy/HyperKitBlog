package org.librazy.demo.dubbo.test;

import com.alibaba.dubbo.config.annotation.Reference;
import com.bitbucket.thinbus.srp6.js.HexHashedVerifierGenerator;
import com.bitbucket.thinbus.srp6.js.SRP6JavaClientSessionSHA256;
import com.bitbucket.thinbus.srp6.js.SRP6JavascriptServerSessionSHA256;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.nimbusds.srp6.SRP6Exception;
import org.h2.tools.Server;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.librazy.demo.dubbo.config.JwtConfigParams;
import org.librazy.demo.dubbo.config.SrpConfigParams;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.*;
import org.librazy.demo.dubbo.service.SrpSessionService;
import org.librazy.demo.dubbo.service.UserService;
import org.librazy.demo.dubbo.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.SocketUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.transaction.Transactional;
import java.lang.reflect.Type;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Rollback
@AutoConfigureMockMvc
@ActiveProfiles("test-default")
class RestApiAndWsTest {

    private static Server h2Server;
    @LocalServerPort
    private int port;
    @Autowired
    private JwtConfigParams jwtConfigParams;
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private SrpConfigParams config;
    @Autowired
    private StatefulRedisConnection<String, String> connection;
    @Autowired(required = false)
    @Reference
    private UserSessionService userSessionService;
    @Autowired(required = false)
    @Reference
    private SrpSessionService srpSessionService;
    @Autowired
    private UserService userService;

    private static MessageDigest sha512() {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("not possible in jdk1.7 and 1.8: ", e);
        }
    }


    @BeforeAll
    static void startH2Console() throws SQLException {
        h2Server = Server.createWebServer("-web",
                "-webAllowOthers", "-webPort", String.valueOf(SocketUtils.findAvailableTcpPort()));
        h2Server.start();
    }

    @AfterAll
    static void stopH2Console() {
        h2Server.stop();
    }

    @BeforeEach
    void cleanRedis() {
        connection.sync().flushall();
    }

    @Test
    @Transactional
    @Rollback
    @SuppressWarnings("unchecked")
    void restApiAndWsTest() throws Exception {
        final String email = "a@b.com";
        final String password = "password";
        final String nick = "nick";

        // init the client session and parameters
        SRP6JavaClientSessionSHA256 signupSession = new SRP6JavaClientSessionSHA256(config.n, config.g);
        signupSession.step1(email, password);

        String salt = signupSession.generateRandomSalt(SRP6JavascriptServerSessionSHA256.HASH_BYTE_LENGTH);

        HexHashedVerifierGenerator gen = new HexHashedVerifierGenerator(
                config.n, config.g, SRP6JavascriptServerSessionSHA256.SHA_256);
        String verifier = gen.generateVerifier(salt, email, password);


        // perform a signup, we should get 202 and parameters will be cached by the server
        // we need to perform a register to validate it
        SrpSignupForm signupForm = new SrpSignupForm();
        signupForm.setEmail(email);
        signupForm.setSalt(salt);
        signupForm.setVerifier(verifier);
        signupForm.setNick(nick);
        SrpChallengeForm challengeForm = new SrpChallengeForm();
        challengeForm.setEmail(email);

        ResponseEntity<Map> code = testRestTemplate.postForEntity("/code", challengeForm, Map.class);
        assertNotNull(code.getBody());
        assertEquals("ok", code.getBody().get("status"));
        assertNotNull(code.getBody().get("mock"));
        String mockCode = (String) code.getBody().get("mock");

        ResponseEntity<Map> codeReplay = testRestTemplate.postForEntity("/code", challengeForm, Map.class);
        assertNotNull(codeReplay.getBody());
        assertEquals("error", codeReplay.getBody().get("status"));
        assertNull(codeReplay.getBody().get("mock"));

        SrpChallengeForm invalidChallengeForm = new SrpChallengeForm();
        invalidChallengeForm.setEmail("invalid email");
        ResponseEntity<Map> codeInvalid = testRestTemplate.postForEntity("/code", invalidChallengeForm, Map.class);
        assertNotNull(codeInvalid.getBody());
        assertEquals("error", codeInvalid.getBody().get("status"));
        assertNull(codeInvalid.getBody().get("mock"));

        signupForm.setCode("badcode");
        ResponseEntity<Map> badSignup = testRestTemplate.postForEntity("/signup", signupForm, Map.class);
        assertEquals(409, badSignup.getStatusCodeValue());

        signupForm.setCode(mockCode);
        ResponseEntity<Map> signup = testRestTemplate.postForEntity("/signup", signupForm, Map.class);
        assertEquals(202, signup.getStatusCodeValue());
        Map<String, String> signupBody = signup.getBody();
        assertNotNull(signupBody);
        assertEquals("ok", signupBody.get("status"));
        assertNotNull(signupBody.get("id"));
        assertNotNull(signupBody.get("salt"));
        assertNotNull(signupBody.get("b"));

        // got server reply with our signup parameters
        signupSession.step2(signupBody.get("salt"), signupBody.get("b"));
        SrpRegisterForm registerForm = new SrpRegisterForm();
        registerForm.setId(Long.valueOf(signupBody.get("id")));
        assertTrue(registerForm.getId() < 0);
        registerForm.setPassword(signupSession.getClientEvidenceMessage() + ":" + signupSession.getPublicClientValue());

        ResponseEntity<Map> signupReplay = testRestTemplate.postForEntity("/signup", signupForm, Map.class);
        assertEquals(400, signupReplay.getStatusCodeValue());

        // a anonymous request fails with 401
        ResponseEntity<Void> mustFail401 = testRestTemplate.getForEntity("/204", Void.class);
        assertEquals(401, mustFail401.getStatusCodeValue());

        // perform a register to validate our signup params
        ResponseEntity<Map> register = testRestTemplate.postForEntity("/register", registerForm, Map.class);
        assertEquals(201, register.getStatusCodeValue());
        Map<String, String> registerBody = register.getBody();
        assertNotNull(registerBody);
        assertEquals("ok", registerBody.get("status"));
        assertNotNull(registerBody.get("id"));
        assertNotNull(registerBody.get("m2"));
        signupSession.step3(registerBody.get("m2"));
        signupSession.getSessionKey(false);

        // then try signin
        SRP6JavaClientSessionSHA256 signinSession = new SRP6JavaClientSessionSHA256(config.n, config.g);
        signinSession.step1(email, password);

        SrpChallengeForm srpChallengeForm = new SrpChallengeForm();
        srpChallengeForm.setEmail(email);

        ResponseEntity<Map> challenge = testRestTemplate.postForEntity("/challenge", srpChallengeForm, Map.class);
        assertEquals(200, challenge.getStatusCodeValue());
        Map<String, String> serverChallenge = challenge.getBody();
        assertNotNull(serverChallenge);
        signinSession.step2(serverChallenge.get("salt"), serverChallenge.get("b"));

        SrpSigninForm srpSigninForm = new SrpSigninForm();
        srpSigninForm.setEmail(email);
        srpSigninForm.setPassword(signinSession.getClientEvidenceMessage() + ":" + signinSession.getPublicClientValue());

        ResponseEntity<Map> signin = testRestTemplate.postForEntity("/authenticate", srpSigninForm, Map.class);
        assertEquals(200, signin.getStatusCodeValue());
        Map<String, String> signinBody = signin.getBody();
        assertNotNull(signinBody);
        assertEquals("ok", signinBody.get("status"));
        assertNotNull(signinBody.get("m2"));
        assertNotNull(signinBody.get("jwt"));

        signinSession.step3(signinBody.get("m2"));
        signinSession.getSessionKey(false);

        // assertNotEquals(userSessionService.getSessions(signinBody.get("id")).size(), 0);

        // the signin session works
        String signinJwt = jwtConfigParams.tokenHead + " " + signinBody.get("jwt");
        ResponseEntity<Void> jwtReqSignin = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, signinJwt).build(), Void.class);
        assertEquals(204, jwtReqSignin.getStatusCodeValue());

        // keys match
        String key = signinSession.getSessionKey(false);
        assertEquals(key, userSessionService.getKey(signinBody.get("id"), signinSession.getSessionKey(true)));

        // and the signup session still there
        String registerJwt = jwtConfigParams.tokenHead + " " + registerBody.get("jwt");
        ResponseEntity<Void> jwtReqReg = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, registerJwt).build(), Void.class);
        assertEquals(204, jwtReqReg.getStatusCodeValue());

        assertNotNull(userSessionService.getUserAgent(registerBody.get("id"), signupSession.getSessionKey(true)));

        // delete sessions should invalid their requests but not the others
        userSessionService.deleteSession(registerBody.get("id"), signupSession.getSessionKey(true));

        ResponseEntity<Void> jwtReqRegDeleted = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, registerJwt).build(), Void.class);
        assertEquals(401, jwtReqRegDeleted.getStatusCodeValue());

        ResponseEntity<Void> jwtReqSigninNotDeleted = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, signinJwt).build(), Void.class);
        assertEquals(204, jwtReqSigninNotDeleted.getStatusCodeValue());


        JwtRefreshForm refreshForm = new JwtRefreshForm();
        refreshForm.setTimestamp(new Date().getTime());

        refreshForm.setSign("badsign");
        ResponseEntity<Map> refreshBadSign = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(400, refreshBadSign.getStatusCodeValue());

        refreshForm.setNonce(UUID.randomUUID().toString());
        Cipher cipherbad = Cipher.getInstance("AES/GCM/NoPadding");
        cipherbad.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha512().digest(key.getBytes()), 0, 32, "AES"), new GCMParameterSpec(96, refreshForm.getNonce().getBytes()));
        String plainbad = refreshForm.getNonce() + String.valueOf(refreshForm.getTimestamp());
        String signbad = Base64.getEncoder().encodeToString(cipherbad.doFinal(plainbad.getBytes()));
        refreshForm.setSign(signbad);
        ResponseEntity<Map> refreshbad = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(401, refreshbad.getStatusCodeValue());

        Cipher cipherreused = Cipher.getInstance("AES/GCM/NoPadding");
        cipherreused.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha512().digest(key.getBytes()), 0, 32, "AES"), new GCMParameterSpec(96, refreshForm.getNonce().getBytes()));
        String plainreused = refreshForm.getNonce() + " " + String.valueOf(refreshForm.getTimestamp());
        String signreused = Base64.getEncoder().encodeToString(cipherreused.doFinal(plainreused.getBytes()));
        refreshForm.setSign(signreused);
        ResponseEntity<Map> refreshreused = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(401, refreshreused.getStatusCodeValue());

        refreshForm.setNonce(UUID.randomUUID().toString());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha512().digest(key.getBytes()), 0, 32, "AES"), new GCMParameterSpec(96, refreshForm.getNonce().getBytes()));
        String plain = refreshForm.getNonce() + " " + String.valueOf(refreshForm.getTimestamp());
        String sign = Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes()));
        refreshForm.setSign(sign);
        ResponseEntity<Map> refresh = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(200, refresh.getStatusCodeValue());

        Map<String, String> refreshBody = refresh.getBody();
        assertNotNull(refreshBody);
        assertNotNull(refreshBody.get("jwt"));
        assertEquals("ok", refreshBody.get("status"));

        ResponseEntity<Void> jwtReqRefresh = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, jwtConfigParams.tokenHead + " " + refreshBody.get("jwt")).build(), Void.class);
        assertEquals(204, jwtReqRefresh.getStatusCodeValue());

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders headers = new StompHeaders();
        headers.put(jwtConfigParams.tokenHeader, Collections.singletonList(signinJwt));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        StompSession stompSession =
                stompClient.connect("ws://localhost:" + port + "/stomp",
                        new WebSocketHttpHeaders(),
                        headers,
                        new StompSessionHandlerAdapter() {
                        }).get();
        final boolean[] messageReceived = {false};
        stompSession.subscribe("/topic/broadcast", new StompFrameHandler() {
            @NotNull
            @Override
            public Type getPayloadType(@NotNull StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                ChatMessage chatMessage = (ChatMessage) payload;
                assertEquals("Content", chatMessage.getContent());
                messageReceived[0] = true;
            }
        });
        stompSession.send("/app/broadcast", new ChatMessage().setMid(1L).setContent("Content"));
        TimeUnit.SECONDS.sleep(1);
        assertTrue(messageReceived[0]);
        userSessionService.clearSession(registerBody.get("id"));

        ResponseEntity<Void> jwtReqSigninDeleted = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, signinJwt).build(), Void.class);
        assertEquals(401, jwtReqSigninDeleted.getStatusCodeValue());

        ResponseEntity<Map> codeReplay2 = testRestTemplate.postForEntity("/code", challengeForm, Map.class);
        assertNotNull(codeReplay2.getBody());
        assertEquals("ok", codeReplay2.getBody().get("status"));
        assertNull(codeReplay2.getBody().get("mock"));
    }

    @Test
    @Transactional
    @Rollback
    void fakeAccountCanChallenge() {
        String email = "someone@notexist.com";
        SrpChallengeForm srpChallengeForm = new SrpChallengeForm();
        srpChallengeForm.setEmail(email);

        ResponseEntity<Map> challenge = testRestTemplate.postForEntity("/challenge", srpChallengeForm, Map.class);
        assertEquals(200, challenge.getStatusCodeValue());
        assertNotNull(challenge.getBody());
    }

    @Test
    @Transactional
    @Rollback
    void fakeAccountCannotAuthenticate() {
        String email = "someone@notexist.com";
        SrpSigninForm srpSigninForm = new SrpSigninForm();
        srpSigninForm.setPassword("somea:somem1");
        srpSigninForm.setEmail(email);
        ResponseEntity<Map> challenge = testRestTemplate.postForEntity("/authenticate", srpSigninForm, Map.class);
        assertEquals(401, challenge.getStatusCodeValue());
    }

    @Test
    @Transactional
    @Rollback
    void invalidPasswordCannotAuthenticate() {
        String email = "someone@notexist.com";
        SrpSigninForm srpSigninForm = new SrpSigninForm();
        srpSigninForm.setPassword("invalid");
        srpSigninForm.setEmail(email);
        ResponseEntity<Map> authenticate = testRestTemplate.postForEntity("/authenticate", srpSigninForm, Map.class);
        assertEquals(401, authenticate.getStatusCodeValue());
    }

    @Test
    @Transactional
    @Rollback
    void invalidRequestShould400() {
        ResponseEntity<Map> signup = testRestTemplate.postForEntity("/signup", new BadRequestEntity(), Map.class);
        assertEquals(400, signup.getStatusCodeValue());

        ResponseEntity<Map> register = testRestTemplate.postForEntity("/register", new BadRequestEntity(), Map.class);
        assertEquals(400, register.getStatusCodeValue());

        ResponseEntity<Map> challenge = testRestTemplate.postForEntity("/challenge", new BadRequestEntity(), Map.class);
        assertEquals(400, challenge.getStatusCodeValue());

        ResponseEntity<Map> authenticate = testRestTemplate.postForEntity("/authenticate", new BadRequestEntity(), Map.class);
        assertEquals(400, authenticate.getStatusCodeValue());
    }

    class BadRequestEntity {
        public String bad;
    }

    @Test
    @Transactional
    @Rollback
    void requestWithNonBearerAuthWill401() {
        ResponseEntity<Void> req = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, "NotABearer").build(), Void.class);
        assertEquals(401, req.getStatusCodeValue());
    }


    @Test
    @Transactional
    @Rollback
    void badWsConnectWithoutAuth() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders headers = new StompHeaders();
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        ListenableFuture<StompSession> connect = stompClient.connect("ws://localhost:" + port + "/stomp",
                new WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {
                });
        ExecutionException exception = assertThrows(ExecutionException.class, connect::get);
        assertEquals(ConnectionLostException.class, exception.getCause().getClass());
    }

    @Test
    void badWsConnectWithBadAuth() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders headers = new StompHeaders();
        headers.put(jwtConfigParams.tokenHeader, Collections.singletonList(jwtConfigParams.tokenHead + "Ba.dTo.ken"));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        ListenableFuture<StompSession> connect = stompClient.connect("ws://localhost:" + port + "/stomp",
                new WebSocketHttpHeaders(),
                headers,
                new StompSessionHandlerAdapter() {
                });
        ExecutionException exception = assertThrows(ExecutionException.class, connect::get);
        assertEquals(ConnectionLostException.class, exception.getCause().getClass());
    }

    @Test
    @Transactional
    @Rollback
    void signupWithoutCode() {
        final String email = "a@b.com";
        final String nick = "nick";

        SrpSignupForm signupForm = new SrpSignupForm();
        signupForm.setEmail(email);
        signupForm.setSalt("some salt");
        signupForm.setVerifier("some verifier");
        signupForm.setNick(nick);
        SrpChallengeForm challengeForm = new SrpChallengeForm();
        challengeForm.setEmail(email);

        signupForm.setCode("badcode");
        ResponseEntity<Map> badSignup = testRestTemplate.postForEntity("/signup", signupForm, Map.class);
        assertEquals(409, badSignup.getStatusCodeValue());
    }

    @Test
    @Transactional
    @Rollback
    void badAndGoodRegister() throws SRP6Exception {
        final String email = "a@b.com";
        final String password = "password";

        // init the client session and parameters
        SRP6JavaClientSessionSHA256 signupSession = new SRP6JavaClientSessionSHA256(config.n, config.g);
        signupSession.step1(email, password);

        String salt = signupSession.generateRandomSalt(SRP6JavascriptServerSessionSHA256.HASH_BYTE_LENGTH);

        HexHashedVerifierGenerator gen = new HexHashedVerifierGenerator(
                config.n, config.g, SRP6JavascriptServerSessionSHA256.SHA_256);
        String verifier = gen.generateVerifier(salt, email, password);

        SrpRegisterForm badPass = new SrpRegisterForm();
        badPass.setId(-1L);
        badPass.setPassword("bad");

        ResponseEntity<Map> badPassRes = testRestTemplate.postForEntity("/register", badPass, Map.class);
        assertEquals(400, badPassRes.getStatusCodeValue());

        SrpRegisterForm noSession = new SrpRegisterForm();
        noSession.setId(-1L);
        noSession.setPassword("ba:d");

        ResponseEntity<Map> noSessionRes = testRestTemplate.postForEntity("/register", noSession, Map.class);
        assertEquals(400, noSessionRes.getStatusCodeValue());

        SrpSignupForm signupForm = new SrpSignupForm();
        signupForm.setEmail(email);
        signupForm.setSalt(salt);
        signupForm.setVerifier(verifier);
        signupForm.setCode("code");
        signupForm.setNick("nick");
        UserEntity user = new UserEntity(-114515L, signupForm.getEmail());
        SrpAccountEntity account = new SrpAccountEntity(user, signupForm.getSalt(), signupForm.getVerifier());
        srpSessionService.newSession(config.n, config.g);
        srpSessionService.step1(account);
        srpSessionService.saveSignup(signupForm);

        SrpRegisterForm badPass2 = new SrpRegisterForm();
        badPass2.setId(-114515L);
        badPass2.setPassword("ba:d");

        ResponseEntity<Map> badPass2Res = testRestTemplate.postForEntity("/register", badPass2, Map.class);
        assertEquals(400, badPass2Res.getStatusCodeValue());
    }
}
