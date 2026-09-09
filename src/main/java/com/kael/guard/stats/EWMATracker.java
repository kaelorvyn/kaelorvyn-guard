package com.kael.guard.stats;

public final class EWMATracker {

    private final double alpha;
    private double mean;
    private double variance;
    private int count;

    public EWMATracker(double alpha) {
        this.alpha = alpha;
    }

    public double updateAndGetZScore(double value) {
        if (count == 0) {
            mean = value;
            variance = 0;
            count++;
            return 0;
        }
        double std = Math.sqrt(variance);
        double z = std == 0 ? 0 : Math.abs(value - mean) / std;
        double diff = value - mean;
        mean += alpha * diff;
        variance = (1 - alpha) * (variance + alpha * diff * diff);
        count++;
        return z;
    }

    public double mean() { return mean; }
    public int count() { return count; }
}
