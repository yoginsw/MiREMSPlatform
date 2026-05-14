package io.mirems.core.domain.result;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Audit event emitted after a vote correction receives two distinct approvals. */
public record VoteCorrectedEvent(
        UUID correctionId,
        UUID originalVotingResultId,
        List<UUID> originalCandidateIds,
        List<UUID> correctedCandidateIds,
        String reason,
        String requestedBy,
        OffsetDateTime requestedAt,
        String firstApprovedBy,
        OffsetDateTime firstApprovedAt,
        String secondApprovedBy,
        OffsetDateTime secondApprovedAt) {
    public VoteCorrectedEvent {
        Objects.requireNonNull(correctionId, "correctionId is required");
        Objects.requireNonNull(originalVotingResultId, "originalVotingResultId is required");
        originalCandidateIds = List.copyOf(Objects.requireNonNull(originalCandidateIds, "originalCandidateIds is required"));
        correctedCandidateIds = List.copyOf(Objects.requireNonNull(correctedCandidateIds, "correctedCandidateIds is required"));
        requireText(reason, "reason");
        requireText(requestedBy, "requestedBy");
        Objects.requireNonNull(requestedAt, "requestedAt is required");
        requireText(firstApprovedBy, "firstApprovedBy");
        Objects.requireNonNull(firstApprovedAt, "firstApprovedAt is required");
        requireText(secondApprovedBy, "secondApprovedBy");
        Objects.requireNonNull(secondApprovedAt, "secondApprovedAt is required");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
