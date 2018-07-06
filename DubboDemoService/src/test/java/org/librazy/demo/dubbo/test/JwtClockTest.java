package org.librazy.demo.dubbo.test;

import org.junit.jupiter.api.Test;
import org.librazy.demo.dubbo.service.JwtTokenServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtClockTest {

    @Test
    void clock() {
        JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(null);
        jwtTokenService.setClock(10000000L);
        assertEquals(10000000L, jwtTokenService.getClock());
    }
}
