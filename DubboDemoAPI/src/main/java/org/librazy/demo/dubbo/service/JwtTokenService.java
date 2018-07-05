package org.librazy.demo.dubbo.service;

import org.librazy.demo.dubbo.domain.UserEntity;

import java.util.Map;

public interface JwtTokenService {

    long getExpiration();

    long getMaximumRefresh();

    Map<String, Object> validateClaimsFromToken(String token);

    String generateToken(UserEntity user, String session);

    String refreshToken(String token);

    long getClock();

    void setClock(Long clock);
}
