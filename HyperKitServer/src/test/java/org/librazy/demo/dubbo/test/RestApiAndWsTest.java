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
import org.librazy.demo.dubbo.config.RedisUtils;
import org.librazy.demo.dubbo.config.SecurityInstanceUtils;
import org.librazy.demo.dubbo.config.SrpConfigParams;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.domain.repo.BlogRepository;
import org.librazy.demo.dubbo.domain.repo.SrpAccountRepository;
import org.librazy.demo.dubbo.domain.repo.UserRepository;
import org.librazy.demo.dubbo.model.*;
import org.librazy.demo.dubbo.service.JwtTokenService;
import org.librazy.demo.dubbo.service.SrpSessionService;
import org.librazy.demo.dubbo.service.UserService;
import org.librazy.demo.dubbo.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.SocketUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityManager;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    private UserService userService;
    @Autowired(required = false)
    @Reference
    private UserSessionService userSessionService;
    @Autowired(required = false)
    @Reference
    private JwtTokenService jwtTokenService;
    @Autowired(required = false)
    @Reference
    private SrpSessionService srpSessionService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SrpAccountRepository srpAccountRepository;
    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeAll
    static void start() throws SQLException {
        h2Server = Server.createWebServer("-web",
                "-webAllowOthers", "-webPort", String.valueOf(SocketUtils.findAvailableTcpPort()));
        h2Server.start();
    }

    @AfterAll
    static void stop() {
        h2Server.stop();
    }

    @BeforeEach
    void clean() {
        connection.sync().flushall();
        srpSessionService.clear();
        entityManager.clear();
        transactionTemplate.execute((status) -> {
            blogRepository.deleteAllInBatch();
            blogRepository.flush();
            srpAccountRepository.deleteAllInBatch();
            srpAccountRepository.flush();
            userRepository.findAll().forEach(
                    userEntity -> {
                        userEntity.getFollowing().forEach(
                                userEntity::removeFollowing
                        );
                        userEntity.getStarredEntries().forEach(
                                userEntity::removeStarredEntry
                        );
                    });
            userRepository.flush();
            userRepository.findAll().forEach(userRepository::delete);
            userRepository.flush();
            status.flush();
            return null;
        });
    }

    @Test
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

        assertThrows(UnsupportedOperationException.class, () -> srpSessionService.getSessionKey(true));
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
        assertEquals(200, code.getStatusCodeValue());
        assertNotNull(code.getBody().get("mock"));
        String mockCode = (String) code.getBody().get("mock");

        ResponseEntity<Map> codeReplay = testRestTemplate.postForEntity("/code", challengeForm, Map.class);
        assertNotNull(codeReplay.getBody());
        assertEquals(429, codeReplay.getStatusCodeValue());
        assertNull(codeReplay.getBody().get("mock"));

        SrpChallengeForm invalidChallengeForm = new SrpChallengeForm();
        invalidChallengeForm.setEmail("invalid email");
        ResponseEntity<Map> codeInvalid = testRestTemplate.postForEntity("/code", invalidChallengeForm, Map.class);
        assertNotNull(codeInvalid.getBody());
        assertEquals(400, codeInvalid.getStatusCodeValue());
        assertNull(codeInvalid.getBody().get("mock"));

        signupForm.setCode("badcode");
        ResponseEntity<Map> badSignup = testRestTemplate.postForEntity("/signup", signupForm, Map.class);
        assertEquals(409, badSignup.getStatusCodeValue());

        signupForm.setCode(mockCode);
        ResponseEntity<SrpChallengeResult> signup = testRestTemplate.postForEntity("/signup", signupForm, SrpChallengeResult.class);
        assertEquals(202, signup.getStatusCodeValue());
        SrpChallengeResult signupBody = signup.getBody();
        assertNotNull(signupBody);
        assertNotNull(signupBody.getSalt());
        assertNotNull(signupBody.getB());

        // got server reply with our signup parameters
        signupSession.step2(signupBody.getSalt(), signupBody.getB());
        SrpRegisterForm registerForm = new SrpRegisterForm();
        registerForm.setId(signupBody.getId());
        assertTrue(registerForm.getId() < 0);
        registerForm.setPassword(signupSession.getClientEvidenceMessage() + ":" + signupSession.getPublicClientValue());

        ResponseEntity<Map> signupReplay = testRestTemplate.postForEntity("/signup", signupForm, Map.class);
        assertEquals(400, signupReplay.getStatusCodeValue());

        // a anonymous request fails with 401
        ResponseEntity<Void> mustFail401 = testRestTemplate.getForEntity("/204", Void.class);
        assertEquals(401, mustFail401.getStatusCodeValue());

        // perform a register to validate our signup params
        ResponseEntity<JwtResult> register = testRestTemplate.postForEntity("/register", registerForm, JwtResult.class);
        assertEquals(201, register.getStatusCodeValue());
        JwtResult registerBody = register.getBody();
        assertNotNull(registerBody);
        assertNotNull(registerBody.getM2());
        signupSession.step3(registerBody.getM2());
        signupSession.getSessionKey(false);

        ResponseEntity<Map> signupAlreadyExist = testRestTemplate.postForEntity("/signup", signupForm, Map.class);
        assertEquals(409, signupAlreadyExist.getStatusCodeValue());

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

        ResponseEntity<Map> challengeReplay = testRestTemplate.postForEntity("/challenge", srpChallengeForm, Map.class);
        assertEquals(400, challengeReplay.getStatusCodeValue());

        SrpSigninForm srpSigninForm = new SrpSigninForm();
        srpSigninForm.setEmail(email);
        srpSigninForm.setPassword(signinSession.getClientEvidenceMessage() + ":" + signinSession.getPublicClientValue());

        ResponseEntity<JwtResult> signin = testRestTemplate.postForEntity("/authenticate", srpSigninForm, JwtResult.class);
        assertEquals(200, signin.getStatusCodeValue());
        JwtResult signinBody = signin.getBody();
        assertNotNull(signinBody);
        assertNotNull(signinBody.getM2());
        assertNotNull(signinBody.getJwt());

        signinSession.step3(signinBody.getM2());
        signinSession.getSessionKey(false);

        // the signin session works
        String signinJwt = jwtConfigParams.tokenHead + " " + signinBody.getJwt();
        ResponseEntity<Void> jwtReqSignin = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, signinJwt).build(), Void.class);
        assertEquals(204, jwtReqSignin.getStatusCodeValue());

        assertThrows(IllegalStateException.class, () -> srpSessionService.loadSession(signinBody.getId()));
        assertThrows(IllegalStateException.class, () -> srpSessionService.confirmSignup(2333));
        assertThrows(IllegalStateException.class, () -> srpSessionService.getSignup());
        assertThrows(IllegalArgumentException.class, () -> srpSessionService.saveSignup(signupForm));

        // keys match
        String key = signinSession.getSessionKey(false);
        String siginId = String.valueOf(signinBody.getId());
        assertEquals(key, userSessionService.getKey(siginId, signinSession.getSessionKey(true)));

        // and the signup session still there
        String registerJwt = jwtConfigParams.tokenHead + " " + registerBody.getJwt();
        ResponseEntity<Void> jwtReqReg = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, registerJwt).build(), Void.class);
        assertEquals(204, jwtReqReg.getStatusCodeValue());

        String registerId = String.valueOf(registerBody.getId());
        assertNotNull(userSessionService.getUserAgent(registerId, signupSession.getSessionKey(true)));

        // delete sessions should invalid their requests but not the others
        userSessionService.deleteSession(registerId, signupSession.getSessionKey(true));

        ResponseEntity<Void> jwtReqRegDeleted = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, registerJwt).build(), Void.class);
        assertEquals(401, jwtReqRegDeleted.getStatusCodeValue());

        ResponseEntity<Void> jwtReqSigninNotDeleted = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, signinJwt).build(), Void.class);
        assertEquals(204, jwtReqSigninNotDeleted.getStatusCodeValue());


        JwtRefreshForm refreshForm = new JwtRefreshForm();
        refreshForm.setTimestamp(jwtTokenService.getClock());

        refreshForm.setSign("badsign");
        ResponseEntity<Map> refreshBadSign = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(400, refreshBadSign.getStatusCodeValue());

        refreshForm.setNonce(UUID.randomUUID().toString());
        Cipher cipherbad = Cipher.getInstance("AES/GCM/NoPadding");
        cipherbad.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SecurityInstanceUtils.getSha512().digest(key.getBytes()), 0, 32, "AES"), new GCMParameterSpec(96, refreshForm.getNonce().getBytes()));
        String plainbad = refreshForm.getNonce() + String.valueOf(refreshForm.getTimestamp());
        String signbad = Base64.getEncoder().encodeToString(cipherbad.doFinal(plainbad.getBytes()));
        refreshForm.setSign(signbad);
        ResponseEntity<Map> refreshbad = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(401, refreshbad.getStatusCodeValue());

        Cipher cipherReused = Cipher.getInstance("AES/GCM/NoPadding");
        cipherReused.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SecurityInstanceUtils.getSha512().digest(key.getBytes()), 0, 32, "AES"), new GCMParameterSpec(96, refreshForm.getNonce().getBytes()));
        String plainReused = refreshForm.getNonce() + " " + String.valueOf(refreshForm.getTimestamp());
        String signReused = Base64.getEncoder().encodeToString(cipherReused.doFinal(plainReused.getBytes()));
        refreshForm.setSign(signReused);
        ResponseEntity<Map> refreshReused = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(401, refreshReused.getStatusCodeValue());

        refreshForm.setNonce(UUID.randomUUID().toString());
        refreshForm.setTimestamp(jwtTokenService.getClock() - 30000);
        Cipher cipherbt = Cipher.getInstance("AES/GCM/NoPadding");
        cipherbt.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SecurityInstanceUtils.getSha512().digest(key.getBytes()), 0, 32, "AES"), new GCMParameterSpec(96, refreshForm.getNonce().getBytes()));
        String plainbt = refreshForm.getNonce() + " " + String.valueOf(refreshForm.getTimestamp());
        String signbt = Base64.getEncoder().encodeToString(cipherbt.doFinal(plainbt.getBytes()));
        refreshForm.setSign(signbt);
        ResponseEntity<Map> refreshbt = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(401, refreshbt.getStatusCodeValue());

        long oldExpiration = jwtTokenService.getExpiration();
        long oldMr = jwtTokenService.getMaximumRefresh();
        jwtTokenService.setMaximumRefresh(0);

        refreshForm.setTimestamp(jwtTokenService.getClock());
        refreshForm.setNonce(UUID.randomUUID().toString());
        Cipher ciphermr = Cipher.getInstance("AES/GCM/NoPadding");
        ciphermr.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SecurityInstanceUtils.getSha512().digest(key.getBytes()), 0, 32, "AES"), new GCMParameterSpec(96, refreshForm.getNonce().getBytes()));
        String plainmr = refreshForm.getNonce() + " " + String.valueOf(refreshForm.getTimestamp());
        String signmr = Base64.getEncoder().encodeToString(ciphermr.doFinal(plainmr.getBytes()));
        refreshForm.setSign(signmr);
        ResponseEntity<Map> refreshmr = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(403, refreshmr.getStatusCodeValue());

        jwtTokenService.setExpiration(oldExpiration);
        jwtTokenService.setMaximumRefresh(oldMr);

        refreshForm.setNonce(UUID.randomUUID().toString());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(SecurityInstanceUtils.getSha512().digest(key.getBytes()), 0, 32, "AES"), new GCMParameterSpec(96, refreshForm.getNonce().getBytes()));
        String plain = refreshForm.getNonce() + " " + String.valueOf(refreshForm.getTimestamp());
        String sign = Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes()));
        refreshForm.setSign(sign);
        ResponseEntity<Map> refresh = testRestTemplate.exchange(RequestEntity.post(URI.create("/refresh")).header(jwtConfigParams.tokenHeader, signinJwt).body(refreshForm), Map.class);
        assertEquals(200, refresh.getStatusCodeValue());

        Map<String, String> refreshBody = refresh.getBody();
        assertNotNull(refreshBody);
        assertNotNull(refreshBody.get("jwt"));

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
        stompSession.send("/app/broadcast", new ChatMessage(1L, ChatMessage.MessageType.TEXT).setContent("Content"));
        TimeUnit.SECONDS.sleep(1);
        assertTrue(messageReceived[0]);

        assertTrue(connection.sync().sismember(RedisUtils.sessions(siginId), signinSession.getSessionKey(true)));
        connection.sync().expire(RedisUtils.key(siginId, signinSession.getSessionKey(true)), 1);
        TimeUnit.SECONDS.sleep(2);
        assertFalse(connection.sync().sismember(RedisUtils.sessions(siginId), signinSession.getSessionKey(true)));

        userSessionService.clearSession(registerId);

        ResponseEntity<Void> jwtReqSigninDeleted = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, signinJwt).build(), Void.class);
        assertEquals(401, jwtReqSigninDeleted.getStatusCodeValue());

        ResponseEntity<Map> codeReplay2 = testRestTemplate.postForEntity("/code", challengeForm, Map.class);
        assertEquals(200, codeReplay2.getStatusCodeValue());
        assertNull(codeReplay2.getBody());
    }

    @Test
    void fakeAccountCanChallenge() {
        String email = "someone@notexist.com";
        SrpChallengeForm srpChallengeForm = new SrpChallengeForm();
        srpChallengeForm.setEmail(email);

        ResponseEntity<Map> challenge = testRestTemplate.postForEntity("/challenge", srpChallengeForm, Map.class);
        assertEquals(200, challenge.getStatusCodeValue());
        assertNotNull(challenge.getBody());
    }

    @Test
    void fakeAccountCannotAuthenticate() {
        String email = "someone@notexist.com";
        SrpSigninForm srpSigninForm = new SrpSigninForm();
        srpSigninForm.setPassword("somea:somem1");
        srpSigninForm.setEmail(email);
        ResponseEntity<Map> challenge = testRestTemplate.postForEntity("/authenticate", srpSigninForm, Map.class);
        assertEquals(401, challenge.getStatusCodeValue());
    }

    @Test
    void invalidPasswordCannotAuthenticate() {
        String email = "someone@notexist.com";
        SrpSigninForm srpSigninForm = new SrpSigninForm();
        srpSigninForm.setPassword("invalid");
        srpSigninForm.setEmail(email);
        ResponseEntity<Map> authenticate = testRestTemplate.postForEntity("/authenticate", srpSigninForm, Map.class);
        assertEquals(401, authenticate.getStatusCodeValue());
    }

    @Test
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

    @Test
    void requestWithNonBearerAuthWill401() {
        ResponseEntity<Void> req = testRestTemplate.exchange(RequestEntity.get(URI.create("/204")).header(jwtConfigParams.tokenHeader, "NotABearer").build(), Void.class);
        assertEquals(401, req.getStatusCodeValue());
    }

    @Test
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
    void badWsConnectWithNonBearerAuth() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        StompHeaders headers = new StompHeaders();
        headers.put(jwtConfigParams.tokenHeader, Collections.singletonList("NonBearerAuth"));
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
    void badRegister() throws IOException {
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
        assertThrows(IllegalStateException.class, () -> srpSessionService.saveSignup(signupForm));

        SrpRegisterForm badPass2 = new SrpRegisterForm();
        badPass2.setId(-114515L);
        badPass2.setPassword("ba:d");

        ResponseEntity<Map> badPass2Res = testRestTemplate.postForEntity("/register", badPass2, Map.class);
        assertEquals(400, badPass2Res.getStatusCodeValue());
    }

    @Test
    void goodRegisterWithDupId() throws IOException, SRP6Exception {
        final String email = "c@b.com";
        final String password = "password";

        // init the client session and parameters
        SRP6JavaClientSessionSHA256 signupSession = new SRP6JavaClientSessionSHA256(config.n, config.g);
        signupSession.step1(email, password);

        String salt = signupSession.generateRandomSalt(SRP6JavascriptServerSessionSHA256.HASH_BYTE_LENGTH);

        HexHashedVerifierGenerator gen = new HexHashedVerifierGenerator(
                config.n, config.g, SRP6JavascriptServerSessionSHA256.SHA_256);
        String verifier = gen.generateVerifier(salt, email, password);

        SrpSignupForm signupForm = new SrpSignupForm();
        signupForm.setEmail(email);
        signupForm.setSalt(salt);
        signupForm.setVerifier(verifier);
        signupForm.setCode("code");
        signupForm.setNick("nick");
        UserEntity user = new UserEntity(-114515L, signupForm.getEmail());
        SrpAccountEntity account = new SrpAccountEntity(user, signupForm.getSalt(), signupForm.getVerifier());
        srpSessionService.newSession(config.n, config.g);
        String b = srpSessionService.step1(account);
        srpSessionService.saveSignup(signupForm);
        assertThrows(IllegalStateException.class, () -> srpSessionService.saveSignup(signupForm));

        signupSession.step2(salt, b);
        SrpRegisterForm registerForm = new SrpRegisterForm();
        registerForm.setId(-114515L);
        // lol
        assertTrue(connection.sync().setnx(RedisUtils.sessions("1"), "bad"));
        assertTrue(connection.sync().setnx(RedisUtils.sessions("2"), "bad"));
        assertTrue(connection.sync().setnx(RedisUtils.sessions("3"), "bad"));
        assertTrue(connection.sync().setnx(RedisUtils.sessions("4"), "bad"));
        assertTrue(connection.sync().setnx(RedisUtils.sessions("5"), "bad"));
        assertTrue(connection.sync().setnx(RedisUtils.sessions("6"), "bad"));
        assertTrue(connection.sync().setnx(RedisUtils.sessions("7"), "bad"));
        registerForm.setPassword(signupSession.getClientEvidenceMessage() + ":" + signupSession.getPublicClientValue());
        ResponseEntity<Map> register = testRestTemplate.postForEntity("/register", registerForm, Map.class);
        assertEquals(400, register.getStatusCodeValue());
    }

    @Test
    void blogApiCrudTest() {
        UserEntity testUser = createTestUser();
        UserEntity testUser2 = createTestUser2();
        userSessionService.newSession(testUser.getUsername(), "session", "ua", "key");
        userSessionService.newSession(testUser2.getUsername(), "session", "ua", "key");
        String token = jwtTokenService.generateToken(testUser, "session");
        String token2 = jwtTokenService.generateToken(testUser2, "session");

        BlogEntry blogEntry = new BlogEntry();
        blogEntry.setTitle("Title");
        blogEntry.setContent("Content keyword 1");
        blogEntry.setAuthorId(testUser.getId());
        ResponseEntity<Map> createEntry = testRestTemplate.exchange(RequestEntity.post(URI.create("/blog/")).header(jwtConfigParams.tokenHeader, "Bearer " + token).body(blogEntry), Map.class);
        assertEquals(201, createEntry.getStatusCodeValue());
        URI location = createEntry.getHeaders().getLocation();
        Long id = Long.valueOf((Integer) Objects.requireNonNull(createEntry.getBody()).get("id"));
        assertNotNull(location);
        assertTrue(location.toString().matches("/blog/\\d+/"));

        ResponseEntity<BlogEntry> getEntry = testRestTemplate.exchange(RequestEntity.get(location).header(jwtConfigParams.tokenHeader, "Bearer " + token).build(), BlogEntry.class);
        assertEquals(200, getEntry.getStatusCodeValue());
        ResponseEntity<BlogEntry> getEntry2 = testRestTemplate.exchange(RequestEntity.get(location).header(jwtConfigParams.tokenHeader, "Bearer " + token2).build(), BlogEntry.class);
        assertEquals(200, getEntry2.getStatusCodeValue());

        blogEntry.setContent("Content keyword and 2");
        ResponseEntity<BlogEntry> updateEntry = testRestTemplate.exchange(RequestEntity.put(location).header(jwtConfigParams.tokenHeader, "Bearer " + token).body(blogEntry), BlogEntry.class);
        assertEquals(200, updateEntry.getStatusCodeValue());

        blogEntry.setId(id);
        blogEntry.setContent("Content keyword and 3");
        ResponseEntity<BlogEntry> update2Entry = testRestTemplate.exchange(RequestEntity.put(location).header(jwtConfigParams.tokenHeader, "Bearer " + token).body(blogEntry), BlogEntry.class);
        assertEquals(200, update2Entry.getStatusCodeValue());

        ResponseEntity<List<BlogEntrySearchResult>> search = testRestTemplate.exchange(RequestEntity.get(URI.create("/blog/search?q=keyword")).build(), new ParameterizedTypeReference<List<BlogEntrySearchResult>>() {
        });
        assertEquals(200, search.getStatusCodeValue());
        List<BlogEntrySearchResult> searchResults = search.getBody();
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());
        assertEquals(id, searchResults.get(0).getId());
        assertTrue(searchResults.get(0).getContent().contains("<em>"));

        blogEntry.setId(id + 1);
        blogEntry.setContent("Content keyword and 4");
        ResponseEntity<BlogEntry> update3Entry = testRestTemplate.exchange(RequestEntity.put(location).header(jwtConfigParams.tokenHeader, "Bearer " + token).body(blogEntry), BlogEntry.class);
        assertEquals(400, update3Entry.getStatusCodeValue());

        ResponseEntity<Void> deleteEntryUserNotMatch = testRestTemplate.exchange(RequestEntity.delete(location).header(jwtConfigParams.tokenHeader, "Bearer " + token2).build(), Void.class);
        assertEquals(403, deleteEntryUserNotMatch.getStatusCodeValue());
        ResponseEntity<Void> deleteEntry = testRestTemplate.exchange(RequestEntity.delete(location).header(jwtConfigParams.tokenHeader, "Bearer " + token).build(), Void.class);
        assertEquals(204, deleteEntry.getStatusCodeValue());
        ResponseEntity<Void> deleteNonExistEntry = testRestTemplate.exchange(RequestEntity.delete(location).header(jwtConfigParams.tokenHeader, "Bearer " + token).build(), Void.class);
        assertEquals(404, deleteNonExistEntry.getStatusCodeValue());
    }

    @Test
    void userApiTest() {
        UserEntity testUser = createTestUser();
        UserEntity testUser2 = createTestUser2();
        userSessionService.newSession(testUser.getUsername(), "session", "ua", "key");
        userSessionService.newSession(testUser2.getUsername(), "session", "ua", "key");
        String token = jwtTokenService.generateToken(testUser, "session");
        String token2 = jwtTokenService.generateToken(testUser2, "session");

        // 关注成功
        ResponseEntity<Void> addFollowing = testRestTemplate.exchange(RequestEntity.put(URI.create("/user/" + testUser.getId() + "/following/" + testUser2.getId() + "/")).header(jwtConfigParams.tokenHeader, "Bearer " + token).build(), Void.class);
        assertEquals(201, addFollowing.getStatusCodeValue());

        // 不允许修改他人关注
        ResponseEntity<Void> addFollowingBadToken = testRestTemplate.exchange(RequestEntity.put(URI.create("/user/" + testUser.getId() + "/following/" + testUser2.getId() + "/")).header(jwtConfigParams.tokenHeader, "Bearer " + token2).build(), Void.class);
        assertEquals(403, addFollowingBadToken.getStatusCodeValue());

        // 不允许关注自己
        ResponseEntity<Void> addFollowingSelf = testRestTemplate.exchange(RequestEntity.put(URI.create("/user/" + testUser.getId() + "/following/" + testUser.getId() + "/")).header(jwtConfigParams.tokenHeader, "Bearer " + token).build(), Void.class);
        assertEquals(400, addFollowingSelf.getStatusCodeValue());

        // 不允许重复关注
        ResponseEntity<Void> addFollowing2 = testRestTemplate.exchange(RequestEntity.put(URI.create("/user/" + testUser.getId() + "/following/" + testUser2.getId() + "/")).header(jwtConfigParams.tokenHeader, "Bearer " + token).build(), Void.class);
        assertEquals(409, addFollowing2.getStatusCodeValue());

        // 删除关注成功
        ResponseEntity<Void> deleteFollowing = testRestTemplate.exchange(RequestEntity.delete(URI.create("/user/" + testUser.getId() + "/following/" + testUser2.getId() + "/")).header(jwtConfigParams.tokenHeader, "Bearer " + token).build(), Void.class);
        assertEquals(204, deleteFollowing.getStatusCodeValue());

        // 不允许重复删除关注
        ResponseEntity<Void> deleteFollowing2 = testRestTemplate.exchange(RequestEntity.delete(URI.create("/user/" + testUser.getId() + "/following/" + testUser2.getId() + "/")).header(jwtConfigParams.tokenHeader, "Bearer " + token).build(), Void.class);
        assertEquals(409, deleteFollowing2.getStatusCodeValue());
    }

    @Test
    void createSpringfoxSwaggerJson() throws Exception {
        String outputDir = System.getProperty("io.springfox.staticdocs.outputDir");
        ResponseEntity<String> responseEntity = testRestTemplate.getForEntity("/v2/api-docs", String.class);
        assertEquals(200, responseEntity.getStatusCodeValue());

        String swaggerJson = responseEntity.getBody();
        Files.createDirectories(Paths.get(outputDir));
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputDir, "swagger.json"), StandardCharsets.UTF_8)){
            writer.write(swaggerJson);
        }
    }

    private UserEntity createTestUser() {
        final String nick = "test";
        final String email = "c@b.test";

        SrpSignupForm signupForm = new SrpSignupForm();
        signupForm.setNick(nick);
        signupForm.setEmail(email);
        signupForm.setVerifier("some");
        signupForm.setSalt("salt");
        signupForm.setCode("1234");
        return userService.registerUser(signupForm);
    }

    private UserEntity createTestUser2() {
        final String nick = "test2";
        final String email = "c2@b.test";

        SrpSignupForm signupForm = new SrpSignupForm();
        signupForm.setNick(nick);
        signupForm.setEmail(email);
        signupForm.setVerifier("some2");
        signupForm.setSalt("salt2");
        signupForm.setCode("1334");
        return userService.registerUser(signupForm);
    }

    class BadRequestEntity {
        @SuppressWarnings("unused")
        public String bad;
    }
}
