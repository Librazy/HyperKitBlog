package org.librazy.demo.dubbo.web;

import com.alibaba.dubbo.config.annotation.Reference;
import com.bitbucket.thinbus.srp6.js.HexHashedRoutines;
import com.bitbucket.thinbus.srp6.js.SRP6JavascriptServerSession;
import com.bitbucket.thinbus.srp6.js.SRP6JavascriptServerSessionSHA256;
import com.nimbusds.srp6.BigIntegerUtils;
import org.librazy.demo.dubbo.config.SecurityInstanceUtils;
import org.librazy.demo.dubbo.config.SrpConfigParams;
import org.librazy.demo.dubbo.domain.SrpAccountEntity;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.SrpChallengeForm;
import org.librazy.demo.dubbo.model.SrpRegisterForm;
import org.librazy.demo.dubbo.model.SrpSigninForm;
import org.librazy.demo.dubbo.model.SrpSignupForm;
import org.librazy.demo.dubbo.service.JwtTokenService;
import org.librazy.demo.dubbo.service.SrpSessionService;
import org.librazy.demo.dubbo.service.UserService;
import org.librazy.demo.dubbo.service.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SrpController {

    private static final String STATUS = "status";

    private static final String OK = "ok";

    private static final String ERROR = "error";

    private static final String MSG = "msg";

    private static Logger logger = LoggerFactory.getLogger(SrpController.class);

    private final SrpConfigParams config;

    private final UserService userService;

    @Reference
    private JwtTokenService jwtTokenService;

    @Reference
    private SrpSessionService session;

    @Reference
    private UserSessionService userSessionService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public SrpController(SrpConfigParams config, UserService userService, @Autowired(required = false) JwtTokenService jwtTokenService, @Autowired(required = false) SrpSessionService session, @Autowired(required = false) UserSessionService userSessionService) {
        this.config = config;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.session = session;
        this.userSessionService = userSessionService;
    }

    @PostMapping(value = "signup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SrpSignupForm signupForm,
            Errors errors) {
        Map<String, String> body = new HashMap<>();
        if (errors.hasErrors()) {
            body.put(STATUS, ERROR);
            body.put(MSG, errors.getFieldErrors().stream().reduce("", (s, e) -> s + e.getField() + ";", String::concat));
            return ResponseEntity.badRequest().body(body);
        }
        // never tell user if a email is registered or not
        if (userService.findByEmail(signupForm.getEmail()) != null || !userSessionService.checkCode(signupForm.getEmail(), signupForm.getCode())) {
            body.put(STATUS, ERROR);
            body.put(MSG, "code;");
            return ResponseEntity.status(409).body(body);
        }
        try {
            long fakeid = -Math.abs(BigIntegerUtils.bigIntegerFromBytes(SecurityInstanceUtils.getSha256().digest(signupForm.getEmail().getBytes())).longValue());
            UserEntity user = new UserEntity(fakeid, signupForm.getEmail());
            SrpAccountEntity account = new SrpAccountEntity(user, signupForm.getSalt(), signupForm.getVerifier());
            session.newSession(config.n, config.g);
            String b = session.step1(account);
            session.saveSignup(signupForm);
            body.put(STATUS, OK);
            body.put("id", Long.toString(fakeid));
            body.put("salt", account.getSalt());
            body.put("b", b);
            return ResponseEntity.status(202).body(body);
        } catch (Exception e) {
            logger.warn("Invalid srp signup received", e);
            body.put(STATUS, ERROR);
            body.put(MSG, ";invalid srp signup");
            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping(value = "register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> register(
            @RequestHeader("User-Agent") String userAgent,
            @Valid @RequestBody SrpRegisterForm srpRegisterForm,
            Errors errors) {
        Map<String, String> body = new HashMap<>();
        if (errors.hasErrors()) {
            body.put(STATUS, ERROR);
            body.put(MSG, errors.getFieldErrors().stream().reduce("", (s, e) -> s + e.getField() + ";", String::concat));
            return ResponseEntity.badRequest().body(body);
        }
        try {
            final String[] arrayAandM1 = srpRegisterForm.getPassword().split(":");
            if (arrayAandM1.length == 2) {
                final String M1 = arrayAandM1[0];
                final String A = arrayAandM1[1];
                session.loadSession(srpRegisterForm.getId());
                String m2 = session.step2(A, M1, userAgent);
                SrpSignupForm signupForm = session.getSignup();
                UserEntity user = userService.registerUser(signupForm);
                session.confirmSignup(user.getId());
                body.put(STATUS, OK);
                body.put("id", Long.toString(user.getId()));
                body.put("m2", m2);
                String token = jwtTokenService.generateToken(user, session.getSessionKey(true));
                body.put("jwt", token);
                return ResponseEntity.status(201).body(body);
            } else throw new IllegalArgumentException();
        } catch (Exception e) {
            logger.warn("Invalid srp register received", e);
            body.put(STATUS, ERROR);
            body.put(MSG, ";invalid srp register");
            return ResponseEntity.badRequest().body(body);
        }
    }

    /**
     * Returns the user salt and SRP challenge for the user to perform their password-proof.
     * We don't want to leak to an attacker which users are or are not registered with the site.
     * So we return a fake salt and challenge when there is no such user in the database.
     * The method is deliberately de-optimised to try to make it the same speed for both
     * scenarios.
     */
    @PostMapping(value = "challenge", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Map<String, String>> challenge(
            @Valid @RequestBody SrpChallengeForm challengeForm,
            Errors errors) {
        Map<String, String> body = new HashMap<>();

        if (errors.hasErrors()) {
            body.put(STATUS, ERROR);
            body.put(MSG, errors.getFieldErrors().stream().reduce("", (s, e) -> s + e.getField() + ";", String::concat));
            return ResponseEntity.badRequest().body(body);
        }

        final String fakeSalt = hash(config.saltOfFakeSalt + challengeForm.getEmail());
        final SrpAccountEntity realAccount = userService.getSrpAccount(challengeForm.getEmail());


        if (realAccount != null) {
            session.newSession(config.n, config.g);
            String b = session.step1(realAccount);
            body.put("salt", realAccount.getSalt());
            body.put("b", b);
            return ResponseEntity.ok(body);
        } else {
            final SrpAccountEntity fakeAccount = new SrpAccountEntity(new UserEntity(SecurityInstanceUtils.getStrongRandom().nextLong(), challengeForm.getEmail()), fakeSalt);
            final SRP6JavascriptServerSession fakeSession = new SRP6JavascriptServerSessionSHA256(
                    config.n, config.g);
            String b = fakeSession.step1(fakeAccount.getUser().getEmail(), fakeSalt, "0");
            body.put("salt", fakeSalt);
            body.put("b", b);
            return ResponseEntity.ok(body);
        }
    }

    @PostMapping(value = "authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> authenticate(
            @Valid @RequestBody SrpSigninForm srpSigninForm,
            Errors errors,
            @RequestHeader("User-Agent") String userAgent) {
        Map<String, String> body = new HashMap<>();
        if (errors.hasErrors()) {
            body.put(STATUS, ERROR);
            body.put(MSG, errors.getFieldErrors().stream().reduce("", (s, e) -> s + e.getField() + ";", String::concat));
            return ResponseEntity.badRequest().body(body);
        }
        try {
            final String[] arrayAandM1 = srpSigninForm.getPassword().split(":");
            if (arrayAandM1.length == 2) {
                final String m1 = arrayAandM1[0];
                final String a = arrayAandM1[1];
                final UserEntity ud = userService.findByEmail(srpSigninForm.getEmail());

                if (ud != null) {
                    session.loadSession(ud.getId());
                    String m2 = session.step2(a, m1, userAgent);
                    body.put(STATUS, OK);
                    body.put("m2", m2);
                    body.put("id", Long.toString(ud.getId()));
                    String token = jwtTokenService.generateToken(ud, session.getSessionKey(true));
                    body.put("jwt", token);
                    return ResponseEntity.ok(body);
                } else {
                    body.put(STATUS, ERROR);
                    body.put(MSG, ";username or password error");
                    return ResponseEntity.status(401).body(body);
                }
            } else throw new IllegalArgumentException();
        } catch (Exception e) {
            logger.warn("Authenticate failed with exception", e);
            body.put(STATUS, ERROR);
            body.put(MSG, ";username or password error");
            return ResponseEntity.status(401).body(body);
        }
    }

    private String hash(String value) {
        MessageDigest md = SecurityInstanceUtils.getSha256();
        md.update(value.getBytes(StandardCharsets.UTF_8));
        return HexHashedRoutines.toHexString(md.digest());
    }
}
