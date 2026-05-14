package io.mirems.core.bpmn.correction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record VoteCorrectionRequest(
        UUID correctionId,
        UUID originalVotingResultId,
        List<UUID> correctedCandidateIds,
        String reason,
        String requestedBy,
        OffsetDateTime requestedAt,
        String firstApprover,
        OffsetDateTime firstApprovedAt,
        String secondApprover,
        OffsetDateTime secondApprovedAt) {
    public VoteCorrectionRequest {
        Objects.requireNonNull(correctionId, "correctionId is required");
        Objects.requireNonNull(originalVotingResultId, "originalVotingResultId is required");
        correctedCandidateIds = List.copyOf(Objects.requireNonNull(correctedCandidateIds, "correctedCandidateIds is required"));
        requireText(reason, "reason");
        requireText(requestedBy, "requestedBy");
        Objects.requireNonNull(requestedAt, "requestedAt is required");
        requireText(firstApprover, "firstApprover");
        Objects.requireNonNull(firstApprovedAt, "firstApprovedAt is required");
        requireText(secondApprover, "secondApprover");
        Objects.requireNonNull(secondApprovedAt, "secondApprovedAt is required");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
