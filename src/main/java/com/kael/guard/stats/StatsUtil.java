package com.kael.guard.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class StatsUtil {

    private StatsUtil() {
    }

    public static double mean(Collection<? extends Number> values) {
        if (values.isEmpty()) return 0;
        double sum = 0;
        for (Number n : values) sum += n.doubleValue();
        return sum / values.size();
    }

    public static double stdDev(Collection<? extends Number> values, double mean) {
        if (values.size() < 2) return 0;
        double sum = 0;
        for (Number n : values) {
            double d = n.doubleValue() - mean;
            sum += d * d;
        }
        return Math.sqrt(sum / (values.size() - 1));
    }

    public static double entropy(Collection<? extends Number> values, double binSize) {
        if (values.isEmpty()) return 0;
        Map<Long, Integer> freq = new HashMap<>();
        for (Number n : values) {
            long bin = Math.round(n.doubleValue() / binSize);
            freq.merge(bin, 1, Integer::sum);
        }
        double entropy = 0;
        for (int count : freq.values()) {
            double p = (double) count / values.size();
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    public static double gcd(Collection<? extends Number> values) {
        if (values.size() < 2) return 0;
        double min = Double.MAX_VALUE;
        for (Number n : values) {
            double d = Math.abs(n.doubleValue());
            if (d > 0.0001 && d < min) min = d;
        }
        if (min == Double.MAX_VALUE) return 0;
        if (min < 0.001) min = 0.001;
        double gcd = min;
        for (int i = 0; i < 5; i++) {
            for (Number n : values) {
                double d = Math.abs(n.doubleValue());
                if (d < 0.0001) continue;
                double remainder = d % gcd;
                if (remainder > 0.0001 && remainder < gcd - 0.0001) gcd = remainder;
            }
        }
        return gcd;
    }
}
