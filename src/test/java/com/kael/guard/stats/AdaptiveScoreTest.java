package com.kael.guard.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveScoreTest {

    @Test
    void confidenceRisesWithRepeatedEvidence() {
        AdaptiveScore score = new AdaptiveScore(0.25);
        double first = 0;
        double last = 0;
        for (int i = 0; i < 50; i++) {
            last = score.observe(1.0);
            if (i == 0) first = last;
        }
        assertTrue(last > first, "持续证据应提升置信度");
    }

    @Test
    void confidenceFallsWhenEvidenceStops() {
        AdaptiveScore score = new AdaptiveScore(0.25);
        for (int i = 0; i < 30; i++) score.observe(1.0);
        double high = score.getConfidence();
        for (int i = 0; i < 50; i++) score.observe(0.0);
        assertTrue(score.getConfidence() < high, "正常证据应让置信度回落");
    }
}
