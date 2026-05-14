package io.mirems.core.bpmn.certification;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record ResultCertificationRequest(
        UUID certificateId,
        UUID electionId,
        UUID tabulationReportId,
        String electionAdminReviewer,
        String legalReviewer,
        boolean legalReviewApproved,
        OffsetDateTime certifiedAt) {
    public ResultCertificationRequest {
        Objects.requireNonNull(certificateId, "certificateId is required");
        Objects.requireNonNull(electionId, "electionId is required");
        Objects.requireNonNull(tabulationReportId, "tabulationReportId is required");
        requireText(electionAdminReviewer, "electionAdminReviewer");
        requireText(legalReviewer, "legalReviewer");
        Objects.requireNonNull(certifiedAt, "certifiedAt is required");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
