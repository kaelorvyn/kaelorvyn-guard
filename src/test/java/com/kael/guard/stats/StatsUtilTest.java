package com.kael.guard.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsUtilTest {

    @Test
    void entropyDetectsPeriodicPattern() {
        List<Long> periodic = List.of(50L, 50L, 50L, 50L, 50L, 50L, 50L, 50L);
        List<Long> random = List.of(11L, 73L, 34L, 91L, 22L, 67L, 41L, 88L);
        double ePeriodic = StatsUtil.entropy(periodic, 5.0);
        double eRandom = StatsUtil.entropy(random, 5.0);
        assertTrue(ePeriodic < eRandom, "周期点击熵应显著低于随机点击熵");
    }

    @Test
    void gcdFindsCommonDivisor() {
        List<Float> values = List.of(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f);
        double gcd = StatsUtil.gcd(values);
        assertTrue(Math.abs(gcd - 0.5) < 0.01, "GCD 应接近 0.5，实际 " + gcd);
    }

    @Test
    void stdDevOfConstantIsZero() {
        List<Double> values = List.of(1.0, 1.0, 1.0, 1.0);
        assertTrue(StatsUtil.stdDev(values, 1.0) < 1e-9);
    }
}
