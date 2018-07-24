package org.librazy.demo.dubbo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT HTTP头设置
 */
@Service
public class JwtConfigParams {

    /**
     * Token 所在的 HTTP 头
     */
    @Value("${jwt.header}")
    public String tokenHeader;

    /**
     * Token 类型
     */
    @Value("${jwt.tokenHead}")
    public String tokenHead;

    /**
     * Token 密钥
     */
    @Value("${jwt.secret}")
    public String tokenSecret;
}
