package org.librazy.demo.dubbo.web;

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
import com.alibaba.dubbo.config.annotation.Reference;
import com.bitbucket.thinbus.srp6.js.HexHashedRoutines;
import com.bitbucket.thinbus.srp6.js.SRP6JavascriptServerSession;
import com.bitbucket.thinbus.srp6.js.SRP6JavascriptServerSessionSHA256;
import com.nimbusds.srp6.BigIntegerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SrpController {

    private final SrpConfigParams config;

    private final UserService userService;

    private final MessageDigest md = sha256();

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

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("not possible in jdk1.7 and 1.8: ", e);
        }
    }

    @RequestMapping(value = "signup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SrpSignupForm signupForm,
            Errors errors) {
        Map<String, String> body = new HashMap<>();
        if (errors.hasErrors()) {
            body.put("status", "error");
            body.put("msg", errors.getFieldErrors().stream().reduce("", (s, e) -> s + e.getField() + ";", String::concat));
            return ResponseEntity.badRequest().body(body);
        }
        // never tell user if a email is registered or not
        if (userService.findByEmail(signupForm.getEmail()) != null || !userSessionService.checkCode(signupForm.getEmail(), signupForm.getCode())) {
            body.put("status", "error");
            body.put("msg", "code;");
            return ResponseEntity.status(409).body(body);
        }
        try {
            long fakeid = -Math.abs(BigIntegerUtils.bigIntegerFromBytes(md.digest(signupForm.getEmail().getBytes())).longValue());
            UserEntity user = new UserEntity(fakeid, signupForm.getEmail());
            SrpAccountEntity account = new SrpAccountEntity(user, signupForm.getSalt(), signupForm.getVerifier());
            session.newSession(config.N, config.g);
            String b = session.step1(account);
            session.saveSignup(signupForm);
            body.put("status", "ok");
            body.put("id", Long.toString(fakeid));
            body.put("salt", account.getSalt());
            body.put("b", b);
            return ResponseEntity.status(202).body(body);
        } catch (Exception e) {
            e.printStackTrace();
            body.put("status", "error");
            body.put("msg", ";invalid srp signup");
            return ResponseEntity.badRequest().body(body);
        }
    }

    @RequestMapping(value = "register", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody SrpRegisterForm srpRegisterForm,
            @RequestHeader("User-Agent") String userAgent,
            Errors errors) {
        Map<String, String> body = new HashMap<>();
        if (errors.hasErrors()) {
            body.put("status", "error");
            body.put("msg", errors.getFieldErrors().stream().reduce("", (s, e) -> s + e.getField() + ";", String::concat));
            return ResponseEntity.badRequest().body(body);
        }
        try {
            final String[] arrayAandM1 = srpRegisterForm.getPassword().split(":");
            if (arrayAandM1.length == 2) {
                final String M1 = arrayAandM1[0];
                final String A = arrayAandM1[1];
                session.loadSession(srpRegisterForm.getId());
                String M2 = session.step2(A, M1, userAgent);
                SrpSignupForm signupForm = session.getSignup();
                UserEntity user = userService.registerUser(signupForm);
                session.confirmSignup(user.getId());
                body.put("status", "ok");
                body.put("id", Long.toString(user.getId()));
                body.put("m2", M2);
                String token = jwtTokenService.generateToken(user, session.getSessionKey(true));
                body.put("jwt", token);
                return ResponseEntity.status(201).body(body);
            } else throw new RuntimeException();
        } catch (Exception e) {
            e.printStackTrace();
            body.put("status", "error");
            body.put("msg", ";invalid srp register");
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
    @RequestMapping(value = "challenge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Map<String, String>> challenge(@Valid @RequestBody SrpChallengeForm challengeForm) {
        final String fakeSalt = hash(config.saltOfFakeSalt + challengeForm.getEmail());
        final SrpAccountEntity realAccount = userService.getSrpAccount(challengeForm.getEmail());

        Map<String, String> body = new HashMap<>();

        if (realAccount != null) {
            session.newSession(config.N, config.g);
            String b = session.step1(realAccount);
            body.put("salt", realAccount.getSalt());
            body.put("b", b);
            return ResponseEntity.ok(body);
        } else {
            try {
                final SrpAccountEntity fakeAccount = new SrpAccountEntity(new UserEntity(SecureRandom.getInstanceStrong().nextLong(), challengeForm.getEmail()), fakeSalt);
                final SRP6JavascriptServerSession fakeSession = new SRP6JavascriptServerSessionSHA256(
                        config.N, config.g);
                String b = fakeSession.step1(fakeAccount.getUser().getEmail(), fakeSalt, "0");
                body.put("salt", fakeSalt);
                body.put("b", b);
                return ResponseEntity.ok(body);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @RequestMapping(value = "authenticate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> authenticate(
            @Valid @RequestBody SrpSigninForm srpSigninForm,
            @RequestHeader("User-Agent") String userAgent,
            Errors errors) {
        Map<String, String> body = new HashMap<>();
        if (errors.hasErrors()) {
            body.put("status", "error");
            body.put("msg", errors.getFieldErrors().stream().reduce("", (s, e) -> s + e.getField() + ";", String::concat));
            return ResponseEntity.badRequest().body(body);
        }
        try {
            final String[] arrayAandM1 = srpSigninForm.getPassword().split(":");
            if (arrayAandM1.length == 2) {
                final String M1 = arrayAandM1[0];
                final String A = arrayAandM1[1];
                final UserEntity ud = userService.findByEmail(srpSigninForm.getEmail());

                if (ud != null) {
                    session.loadSession(ud.getId());
                    String m2 = session.step2(A, M1, userAgent);
                    body.put("status", "ok");
                    body.put("m2", m2);
                    body.put("id", Long.toString(ud.getId()));
                    String token = jwtTokenService.generateToken(ud, session.getSessionKey(true));
                    body.put("jwt", token);
                    return ResponseEntity.ok(body);
                } else {
                    body.put("status", "error");
                    body.put("msg", ";username or password error");
                    return ResponseEntity.status(401).body(body);
                }
            } else throw new RuntimeException();
        } catch (Exception e) {
            e.printStackTrace();
            body.put("status", "error");
            body.put("msg", ";username or password error");
            return ResponseEntity.status(401).body(body);
        }
    }

    private String hash(String value) {
        md.update(value.getBytes(StandardCharsets.UTF_8));
        return HexHashedRoutines.toHexString(md.digest());
    }

}
