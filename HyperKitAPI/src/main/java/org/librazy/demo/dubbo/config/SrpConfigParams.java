package org.librazy.demo.dubbo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SRP 协议参数
 */
@Service
public class SrpConfigParams {

    /**
     * 安全素数 N
     */
    @Value("${thinbus.N}")
    public String n;

    /**
     * 基数 G
     */
    @Value("${thinbus.g}")
    public String g;

    /**
     * 假盐的盐
     */
    @Value("${thinbus.salt.of.fake.salt}")
    public String saltOfFakeSalt;
}
