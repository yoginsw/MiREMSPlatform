package io.mirems.core.bpmn.audit;

public record AdvancedAuditReport(
        AdvancedAuditRequest request,
        RiskLimitingAuditPlan riskLimitingAuditPlan,
        StatisticalSample statisticalSample,
        ChainOfCustodyReport chainOfCustodyReport,
        VvsgAuditTrailVerification vvsgVerification,
        ExternalAuditExport jsonExport,
        ExternalAuditExport xmlExport) {}
