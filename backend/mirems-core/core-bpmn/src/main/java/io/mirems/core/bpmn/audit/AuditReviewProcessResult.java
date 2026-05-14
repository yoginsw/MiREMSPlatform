package io.mirems.core.bpmn.audit;

import java.util.Objects;

public record AuditReviewProcessResult(AuditReport report) {
    public AuditReviewProcessResult {
        Objects.requireNonNull(report, "report is required");
    }
}
