package org.librazy.demo.dubbo.web;

import org.librazy.demo.dubbo.config.JwtConfigParams;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.model.JwtRefreshForm;
import org.librazy.demo.dubbo.model.SrpChallengeForm;
import org.librazy.demo.dubbo.service.JwtTokenService;
import org.librazy.demo.dubbo.service.UserService;
import org.librazy.demo.dubbo.service.UserSessionService;
import com.alibaba.dubbo.config.annotation.Reference;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.Valid;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    private final JwtConfigParams jwtConfigParams;

    private final UserService userService;

    @Reference
    private JwtTokenService jwtTokenService;

    @Reference
    private UserSessionService userSessionService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public HomeController(JwtConfigParams jwtConfigParams, UserService userService, @Autowired(required = false) JwtTokenService jwtTokenService, @Autowired(required = false) UserSessionService userSessionService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.jwtConfigParams = jwtConfigParams;
        this.userSessionService = userSessionService;
    }

    private static MessageDigest sha512() {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("not possible in jdk1.7 and 1.8: ", e);
        }
    }

    @RequestMapping(value = "204", method = RequestMethod.GET)
    public ResponseEntity<Void> NoContent() {
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "refresh", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody JwtRefreshForm form, @RequestHeader("Authorization") String auth, Principal sender) {
        String jwt = auth.substring(jwtConfigParams.tokenHead.length() + 1);
        Claims claims = Jwts.claims(jwtTokenService.validateClaimsFromToken(jwt));
        String sid = (String) claims.get("jti");
        String key = userSessionService.getKey(sender.getName(), sid);
        Map<String, String> result = new HashMap<>();
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha512().digest(key.getBytes()), 0, 32, "AES"), new IvParameterSpec(form.getNonce().getBytes(), 0, 16));
            String expected = form.getNonce() + " " + String.valueOf(form.getTimestamp());
            boolean nonceValid = userSessionService.validNonce(form.getNonce());
            String actual = new String(cipher.doFinal(Base64.getDecoder().decode(form.getSign())));
            boolean timeValid = Math.abs(form.getTimestamp() - System.currentTimeMillis()) < 10000;
            boolean signValid = expected.equals(actual);
            if (!timeValid || !signValid || !nonceValid) {
                result.put("status", "error");
                return ResponseEntity.status(401).body(result);
            }
            String newJwt = jwtTokenService.refreshToken(jwt);
            if (newJwt == null) {
                result.put("status", "error");
                return ResponseEntity.status(403).body(result);
            }
            userSessionService.refreshSession(sender.getName(), sid);
            result.put("status", "ok");
            result.put("jwt", newJwt);
            return ResponseEntity.ok(result);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            result.put("status", "error");
            return ResponseEntity.badRequest().body(result);
        }
    }

    // should never fails so you cannot tells if a email is registered
    @RequestMapping(value = "code", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> code(@Valid @RequestBody SrpChallengeForm form) {
        Map<String, String> result = new HashMap<>();
        final UserEntity ud = userService.findByEmail(form.getEmail());

        if (ud != null) {
            // WAI: Prevent spoofing whether one email is already registered
            result.put("status", "ok");
            return ResponseEntity.ok(result);
        }
        try {
            String code = userSessionService.sendCode(form.getEmail());
            if (code != null) {
                result.put("status", "ok");
                result.put("mock", code); // TOD: remove it when email sms api available
                return ResponseEntity.ok(result);
            } else {
                result.put("status", "error");
                result.put("msg", ";too much request");
                return ResponseEntity.status(429).header("Retry-After", "60").body(result);
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("msg", ";email number not valid");
            return ResponseEntity.badRequest().body(result);
        }
    }
}
