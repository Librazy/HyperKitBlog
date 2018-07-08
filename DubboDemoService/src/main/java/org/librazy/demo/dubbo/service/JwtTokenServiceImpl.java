package org.librazy.demo.dubbo.service;

import com.alibaba.dubbo.config.annotation.Service;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultClock;
import io.jsonwebtoken.impl.FixedClock;
import org.librazy.demo.dubbo.domain.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Service(
        interfaceClass = JwtTokenService.class
)
@Component
@org.springframework.stereotype.Service
public class JwtTokenServiceImpl implements JwtTokenService, Serializable {

    private static final long serialVersionUID = -3301605591108950415L;

    private final transient SigningKeyResolverAdapter key;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh.maximum}")
    private Long maximumRefresh;

    private transient Clock clock = new DefaultClock();

    @Autowired
    public JwtTokenServiceImpl(SigningKeyResolverAdapter key) {
        this.key = key;
    }

    @Override
    public long getExpiration() {
        return expiration;
    }

    @Override
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    @Override
    public long getMaximumRefresh() {
        return maximumRefresh;
    }

    @Override
    public void setMaximumRefresh(long maximumRefresh) {
        this.maximumRefresh = maximumRefresh;
    }

    @Override
    public Map<String, Object> validateClaimsFromToken(String token) {
        return Jwts.parser()
                   .setClock(clock)
                   .setSigningKeyResolver(key)
                   .parseClaimsJws(token)
                   .getBody();
    }

    @Override
    public String generateToken(UserEntity user, String session) {
        Date createdDate = clock.now();
        final Date expirationDate = new Date(createdDate.getTime() + expiration * 1000);
        Claims claims = Jwts.claims()
                            .setId(session)
                            .setSubject(String.valueOf(user.getId()))
                            .setIssuedAt(createdDate)
                            .setNotBefore(createdDate)
                            .setExpiration(expirationDate);

        return Jwts.builder()
                   .setClaims(claims)
                   .signWith(SignatureAlgorithm.HS512, key.resolveSigningKey(Jwts.jwsHeader().setAlgorithm(SignatureAlgorithm.HS512.getValue()), claims))
                   .compact();
    }

    @Override
    public String refreshToken(String token) {
        Date currentDate = clock.now();
        final Date expirationDate = new Date(currentDate.getTime() + expiration * 1000);

        final Claims claims = Jwts.claims(validateClaimsFromToken(token));
        if (currentDate.getTime() - claims.getIssuedAt().getTime() > expiration * 1000 * maximumRefresh) {
            return null;
        }
        claims.setExpiration(expirationDate);
        claims.setNotBefore(currentDate);
        return Jwts.builder()
                   .setClaims(claims)
                   .signWith(SignatureAlgorithm.HS512, key.resolveSigningKey(Jwts.jwsHeader().setAlgorithm(SignatureAlgorithm.HS512.getValue()), claims))
                   .compact();
    }

    @Override
    public long getClock() {
        return clock.now().getTime();
    }

    @Override
    public void setClock(Long clock) {
        if (clock == null) {
            this.clock = new DefaultClock();
        } else {
            this.clock = new FixedClock(new Date(clock));
        }
    }
}