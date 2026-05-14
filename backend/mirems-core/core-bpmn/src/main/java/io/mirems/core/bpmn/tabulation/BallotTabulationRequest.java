package io.mirems.core.bpmn.tabulation;

import io.mirems.core.domain.election.ElectionStatus;
import java.util.Objects;
import java.util.UUID;

public record BallotTabulationRequest(
        UUID reportId,
        UUID electionId,
        ElectionStatus electionStatus,
        String reviewerRole,
        boolean publicResults) {
    public BallotTabulationRequest {
        Objects.requireNonNull(reportId, "reportId is required");
        Objects.requireNonNull(electionId, "electionId is required");
        Objects.requireNonNull(electionStatus, "electionStatus is required");
        if (reviewerRole == null || reviewerRole.isBlank()) {
            throw new IllegalArgumentException("reviewerRole is required");
        }
    }
}
