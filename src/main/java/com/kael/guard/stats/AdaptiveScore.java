package com.kael.guard.stats;

public final class AdaptiveScore {

    private final double smoothing;
    private final double decay;
    private double mean;
    private double variance;
    private double score;
    private int samples;

    public AdaptiveScore(double smoothing) {
        this(smoothing, 0.88);
    }

    public AdaptiveScore(double smoothing, double decay) {
        this.smoothing = clamp(smoothing);
        this.decay = clamp(decay);
    }

    public double observe(double evidence) {
        double e = Math.max(0, evidence);
        if (samples == 0) {
            mean = e;
            variance = 0;
            score = e;
            samples = 1;
            return getConfidence();
        }
        double delta = e - mean;
        mean += smoothing * delta;
        variance = (1 - smoothing) * (variance + smoothing * delta * delta);
        double std = Math.sqrt(variance);
        double surprise = std <= 1e-6 ? e : Math.max(0, (e - mean) / (std + 1e-6));
        score = score * decay + e * (1 - decay);
        score += Math.min(0.35, surprise * 0.08);
        samples++;
        return getConfidence();
    }

    public double getConfidence() {
        double sampleBoost = Math.min(0.35, samples / 80.0);
        return 1 - Math.exp(-Math.max(0, score + sampleBoost) * 1.8);
    }

    public double activationGate() {
        double sampleFactor = Math.min(1.0, samples / 48.0);
        return 0.84 - sampleFactor * 0.18;
    }

    public void reset() {
        mean = 0;
        variance = 0;
        score = 0;
        samples = 0;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
