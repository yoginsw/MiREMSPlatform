package io.mirems.core.bpmn.audit;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AdvancedAuditRequest(
        UUID reportId,
        UUID electionId,
        String initiatedBy,
        OffsetDateTime generatedAt,
        int ballotCount,
        int reportedMarginVotes,
        double margin,
        double riskLimit,
        long randomSeed,
        List<String> ballotPopulation) {
    public AdvancedAuditRequest {
        reportId = Objects.requireNonNull(reportId, "reportId is required");
        electionId = Objects.requireNonNull(electionId, "electionId is required");
        initiatedBy = requireText(initiatedBy, "initiatedBy");
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt is required");
        if (ballotCount <= 0) {
            throw new IllegalArgumentException("ballotCount must be positive");
        }
        if (reportedMarginVotes <= 0 || reportedMarginVotes > ballotCount) {
            throw new IllegalArgumentException("reportedMarginVotes must be between 1 and ballotCount");
        }
        if (margin <= 0.0 || margin >= 1.0) {
            throw new IllegalArgumentException("margin must be greater than 0 and less than 1");
        }
        if (riskLimit <= 0.0 || riskLimit >= 1.0) {
            throw new IllegalArgumentException("riskLimit must be greater than 0 and less than 1");
        }
        ballotPopulation = List.copyOf(Objects.requireNonNull(ballotPopulation, "ballotPopulation is required").stream()
                .map(value -> requireText(value, "ballotPopulation item"))
                .distinct()
                .sorted()
                .toList());
        if (ballotPopulation.isEmpty()) {
            throw new IllegalArgumentException("ballotPopulation must not be empty");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
