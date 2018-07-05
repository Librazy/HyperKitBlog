package org.librazy.demo.dubbo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SrpConfigParams {

    @Value("${thinbus.N}")
    public String N;

    @Value("${thinbus.g}")
    public String g;

    @Value("${thinbus.salt.of.fake.salt}")
    public String saltOfFakeSalt;
}
