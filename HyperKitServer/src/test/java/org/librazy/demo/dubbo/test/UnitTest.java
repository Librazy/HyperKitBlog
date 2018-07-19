package org.librazy.demo.dubbo.test;

import com.alibaba.dubbo.config.annotation.Reference;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolver;
import io.jsonwebtoken.UnsupportedJwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.librazy.demo.dubbo.config.RedisUtils;
import org.librazy.demo.dubbo.config.SecurityInstanceUtils;
import org.librazy.demo.dubbo.config.SrpConfigParams;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.service.JwtTokenService;
import org.librazy.demo.dubbo.service.SrpSessionService;
import org.librazy.demo.dubbo.service.UserService;
import org.librazy.demo.dubbo.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test-default")
class UnitTest {

    @Autowired(required = false)
    @Reference
    UserSessionService session;

    @Autowired(required = false)
    @Reference
    SrpSessionService srp;

    @Autowired(required = false)
    @Reference
    JwtTokenService jwtTokenService;

    @Autowired
    SrpConfigParams srpConfigParams;

    @Autowired
    UserService userService;

    @Autowired(required = false)
    @Reference
    SigningKeyResolver jwtKeyResolverService;

    @Autowired
    private StatefulRedisConnection<String, String> connection;

    @Test
    void jwtTokenServiceTest() throws InterruptedException {
        long expMs = jwtTokenService.getExpiration() * 1000;
        long maxR = jwtTokenService.getMaximumRefresh();

        jwtTokenService.setClock(null);
        long clock1 = jwtTokenService.getClock();
        TimeUnit.SECONDS.sleep(1);
        long clock2 = jwtTokenService.getClock();
        assertNotEquals(clock1, clock2);

        long now = new Date().getTime();
        jwtTokenService.setClock(now);
        session.newSession("1", "session.id", "userAgent", "session.key");
        assertThrows(RuntimeException.class, () -> session.newSession("1", "session.id", "userAgent", "session.key"));
        UserEntity user = new UserEntity(1, "user@example.com");
        String token = jwtTokenService.generateToken(user, "session.id");
        Map<String, Object> claims = jwtTokenService.validateClaimsFromToken(token);
        assertEquals("session.id", Jwts.claims(claims).getId());

        //Two second before issue
        jwtTokenService.setClock(now - 200000);
        assertThrows(RuntimeException.class, () -> jwtTokenService.validateClaimsFromToken(token));

        //Two second after issue
        jwtTokenService.setClock(now + 2000);
        String refresh1Token = Objects.requireNonNull(jwtTokenService.refreshToken(token));

        //One second before origin token expires
        jwtTokenService.setClock(now + expMs - 1000);
        jwtTokenService.validateClaimsFromToken(token);
        jwtTokenService.validateClaimsFromToken(refresh1Token);

        //One second after origin token expires
        jwtTokenService.setClock(now + expMs + 1000);
        assertThrows(RuntimeException.class, () -> jwtTokenService.validateClaimsFromToken(token));
        jwtTokenService.validateClaimsFromToken(refresh1Token);

        // Refresh the token...
        long time = expMs - 1000;
        String refreshNToken = refresh1Token;
        for (int i = 0; i < maxR; i++) {
            jwtTokenService.setClock(now + time);
            refreshNToken = Objects.requireNonNull(jwtTokenService.refreshToken(refreshNToken));
            jwtTokenService.validateClaimsFromToken(refreshNToken);
            time += expMs - 1000;
        }

        // ...until maximum refresh exceeded
        jwtTokenService.setClock(now + time);
        jwtTokenService.validateClaimsFromToken(refreshNToken);
        assertNull(jwtTokenService.refreshToken(refreshNToken));

        // And refreshed token expires
        time += expMs - 1000;
        jwtTokenService.setClock(now + time);
        String finalToken = refreshNToken;
        assertThrows(RuntimeException.class, () -> jwtTokenService.validateClaimsFromToken(finalToken));
    }

    @Test
    void jwtDoesNotSupportHS256() {
        assertThrows(UnsupportedJwtException.class, () -> jwtKeyResolverService.resolveSigningKey(Jwts.jwsHeader().setAlgorithm("HS256"), Jwts.claims()));
    }

    @Test
    void srpSessionLoadingFails() {
        assertThrows(IllegalStateException.class, () -> srp.loadSession(114514));
        connection.sync().set(RedisUtils.srpSession(String.valueOf(114514)), "rO0ABXNyAB1vcmcubGlicmF6eS5kZW1vLmR1YmJvLnRlc3QuQ2kuRc/8wB/EAgAAeHA=");
        assertThrows(ClassNotFoundException.class, () -> srp.loadSession(114514));
    }

    @Test
    void utilsIsNotInstantiatable() throws NoSuchMethodException {
        Constructor<RedisUtils> redisUtilsConstructor = RedisUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(redisUtilsConstructor.getModifiers()));
        redisUtilsConstructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, redisUtilsConstructor::newInstance);

        Constructor<SecurityInstanceUtils> securityInstanceUtilsConstructor = SecurityInstanceUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(securityInstanceUtilsConstructor.getModifiers()));
        securityInstanceUtilsConstructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, securityInstanceUtilsConstructor::newInstance);
    }

    @Test
    void loadNonExistUserWillNotFound() {
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("114514"));
    }
}
