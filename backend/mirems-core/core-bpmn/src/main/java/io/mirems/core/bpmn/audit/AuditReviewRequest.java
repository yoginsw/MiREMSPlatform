package io.mirems.core.bpmn.audit;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record AuditReviewRequest(
        UUID reportId,
        UUID electionId,
        String initiatedBy,
        String initiatorRole,
        OffsetDateTime generatedAt) {
    public AuditReviewRequest {
        Objects.requireNonNull(reportId, "reportId is required");
        Objects.requireNonNull(electionId, "electionId is required");
        requireText(initiatedBy, "initiatedBy");
        requireText(initiatorRole, "initiatorRole");
        Objects.requireNonNull(generatedAt, "generatedAt is required");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
