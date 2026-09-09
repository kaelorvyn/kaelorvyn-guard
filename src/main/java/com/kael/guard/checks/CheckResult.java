package com.kael.guard.checks;

import java.util.Map;

public final class CheckResult {

    public enum Source {
        BEHAVIOR,
        AUTOMATIC_AI,
        MANUAL_REVIEW,
        MOD_AUDIT,
        ADMIN
    }

    private final String category;
    private final double confidence;
    private final String reason;
    private final String family;
    private final Map<String, Object> evidence;
    private final boolean signatureOrAntibody;
    private final boolean aiConfirmed;
    private final String serverReason;
    private final Source source;

    public CheckResult(String category, String family, double confidence, String reason,
                       Map<String, Object> evidence, boolean signatureOrAntibody) {
        this(category, family, confidence, reason, evidence, signatureOrAntibody, false, null,
                Source.BEHAVIOR);
    }

    public CheckResult(String category, String family, double confidence, String reason,
                       Map<String, Object> evidence, boolean signatureOrAntibody, boolean aiConfirmed) {
        this(category, family, confidence, reason, evidence, signatureOrAntibody, aiConfirmed, null,
                aiConfirmed ? Source.AUTOMATIC_AI : Source.BEHAVIOR);
    }

    public CheckResult(String category, String family, double confidence, String reason,
                       Map<String, Object> evidence, boolean signatureOrAntibody, boolean aiConfirmed,
                       String serverReason) {
        this(category, family, confidence, reason, evidence, signatureOrAntibody, aiConfirmed, serverReason,
                aiConfirmed ? Source.AUTOMATIC_AI : Source.BEHAVIOR);
    }

    public CheckResult(String category, String family, double confidence, String reason,
                       Map<String, Object> evidence, boolean signatureOrAntibody, boolean aiConfirmed,
                       String serverReason, Source source) {
        this.category = category;
        this.family = family;
        this.confidence = confidence;
        this.reason = reason;
        this.evidence = evidence;
        this.signatureOrAntibody = signatureOrAntibody;
        this.aiConfirmed = aiConfirmed;
        this.serverReason = serverReason;
        this.source = source == null ? Source.BEHAVIOR : source;
    }

    public static CheckResult of(String category, double confidence, String reason) {
        return new CheckResult(category, "", confidence, reason, Map.of(), false);
    }

    public static CheckResult of(String category, double confidence, String reason, Map<String, Object> evidence) {
        return new CheckResult(category, "", confidence, reason, evidence, false);
    }

    public static CheckResult signature(String category, double confidence, String reason) {
        return new CheckResult(category, category, confidence, reason, Map.of(), true);
    }

    public static CheckResult signature(String category, double confidence, String reason, Map<String, Object> evidence) {
        return new CheckResult(category, category, confidence, reason, evidence, true);
    }

    public static CheckResult signature(String category, String family, double confidence, String reason) {
        return new CheckResult(category, family, confidence, reason, Map.of(), true);
    }

    public static CheckResult signature(String category, String family, double confidence,
                                        String reason, Map<String, Object> evidence) {
        return new CheckResult(category, family, confidence, reason, evidence, true);
    }

    public static CheckResult aiConfirmed(String category, double confidence, String reason) {
        return aiConfirmed(category, confidence, reason, reason);
    }

    public static CheckResult aiConfirmed(String category, double confidence, String reason, String serverReason) {
        return new CheckResult(category, "", confidence, reason, Map.of(), false, true, serverReason);
    }

    public static CheckResult manualReview(String category, double confidence, String reason, String serverReason) {
        return new CheckResult(category, "", confidence, reason, Map.of(), false, true, serverReason,
                Source.MANUAL_REVIEW);
    }

    public static CheckResult modAudit(String category, double confidence, String reason) {
        return new CheckResult(category, "", confidence, reason, Map.of(), false, true, reason,
                Source.MOD_AUDIT);
    }

    public static CheckResult adminWarning(String category, double confidence, String reason) {
        return new CheckResult(category, "", confidence, reason, Map.of(), false, true, reason,
                Source.ADMIN);
    }

    public String category() { return category; }
    public String family() { return family; }
    public double confidence() { return confidence; }
    public String reason() { return reason; }
    public Map<String, Object> evidence() { return evidence; }
    public boolean signatureOrAntibody() { return signatureOrAntibody; }
    public boolean aiConfirmed() { return aiConfirmed; }
    public String serverReason() { return serverReason == null ? reason : serverReason; }
    public Source source() { return source; }
}
