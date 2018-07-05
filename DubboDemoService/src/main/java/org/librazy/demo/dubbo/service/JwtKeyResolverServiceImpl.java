package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.config.JwtConfigParams;
import com.alibaba.dubbo.config.annotation.Service;
import com.bitbucket.thinbus.srp6.js.HexHashedRoutines;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Service(
        interfaceClass = SigningKeyResolver.class
)
@Component
public class JwtKeyResolverServiceImpl extends SigningKeyResolverAdapter {

    private final UserSessionService session;

    private final JwtConfigParams jwtConfigParams;

    @Autowired
    public JwtKeyResolverServiceImpl(UserSessionService session, JwtConfigParams jwtConfigParams) {
        this.session = session;
        this.jwtConfigParams = jwtConfigParams;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public byte[] resolveSigningKeyBytes(JwsHeader header, Claims claims) throws RuntimeException {
        return resolveSigningKeyBytes(header, (Map<String, Object>) claims);
    }

    @SuppressWarnings("rawtypes")
    private byte[] resolveSigningKeyBytes(JwsHeader header, Map<String, Object> claims) throws RuntimeException {
        if (SignatureAlgorithm.forName(header.getAlgorithm()) != SignatureAlgorithm.HS512) {
            throw new UnsupportedJwtException("alg not supported");
        }
        Claims c = Jwts.claims(claims);
        String id = c.getSubject();
        String sid = c.getId();
        String key = session.getKey(id, sid);
        if (key == null) {
            throw new RuntimeException();
        }
        String k = key.concat(jwtConfigParams.tokenSecret);
        return k.getBytes(HexHashedRoutines.utf8);
    }
}
