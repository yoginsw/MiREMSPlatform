package io.mirems.core.bpmn.audit;

public record RiskLimitingAuditPlan(
        int ballotCount,
        int reportedMarginVotes,
        double margin,
        double riskLimit,
        int recommendedSampleSize) {}
