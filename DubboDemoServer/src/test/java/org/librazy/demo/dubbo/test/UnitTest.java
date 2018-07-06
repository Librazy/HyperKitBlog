package org.librazy.demo.dubbo.test;

import com.alibaba.dubbo.config.annotation.Reference;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.librazy.demo.dubbo.service.JwtTokenService;
import org.librazy.demo.dubbo.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

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
    JwtTokenService jwtTokenService;

    @Test
    void jwtTokenServiceTest() {
        long expMs = jwtTokenService.getExpiration() * 1000;
        long maxR = jwtTokenService.getMaximumRefresh();
        long now = new Date().getTime();
        jwtTokenService.setClock(now);
        session.newSession("1", "session.id", "UserAgent", "session.key");
        assertThrows(RuntimeException.class, () -> session.newSession("1", "session.id", "UserAgent", "session.key"));
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
}
