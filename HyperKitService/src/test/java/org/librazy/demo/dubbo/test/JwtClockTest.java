package org.librazy.demo.dubbo.test;

import org.junit.jupiter.api.Test;
import org.librazy.demo.dubbo.service.JwtTokenServiceImpl;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JwtClockTest {

    @Test
    void clock() throws InterruptedException {
        JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(null);
        jwtTokenService.setClock(10000000L);
        assertEquals(10000000L, jwtTokenService.getClock());

        jwtTokenService.setClock(null);
        long clock1 = jwtTokenService.getClock();
        TimeUnit.SECONDS.sleep(1);
        long clock2 = jwtTokenService.getClock();
        assertNotEquals(clock1, clock2);
    }
}
