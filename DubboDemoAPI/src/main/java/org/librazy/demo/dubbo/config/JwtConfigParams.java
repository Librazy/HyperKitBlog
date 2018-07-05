package org.librazy.demo.dubbo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtConfigParams {

    @Value("${jwt.header}")
    public String tokenHeader;

    @Value("${jwt.tokenHead}")
    public String tokenHead;

    @Value("${jwt.secret}")
    public String tokenSecret;
}
