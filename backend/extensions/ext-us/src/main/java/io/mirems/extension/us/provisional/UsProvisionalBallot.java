package io.mirems.extension.us.provisional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record UsProvisionalBallot(
        UUID id,
        UUID voterId,
        UUID electionId,
        UUID ballotId,
        String reasonCode,
        UsProvisionalBallotStatus status,
        OffsetDateTime castAt,
        Optional<OffsetDateTime> resolvedAt,
        Optional<String> resolvedBy,
        List<String> auditTrail) {
    public UsProvisionalBallot {
        id = Objects.requireNonNull(id, "id is required");
        voterId = Objects.requireNonNull(voterId, "voterId is required");
        electionId = Objects.requireNonNull(electionId, "electionId is required");
        ballotId = Objects.requireNonNull(ballotId, "ballotId is required");
        reasonCode = normalizeReason(reasonCode);
        status = Objects.requireNonNull(status, "status is required");
        castAt = Objects.requireNonNull(castAt, "castAt is required");
        resolvedAt = Objects.requireNonNull(resolvedAt, "resolvedAt is required");
        resolvedBy = Objects.requireNonNull(resolvedBy, "resolvedBy is required").map(String::trim).filter(value -> !value.isEmpty());
        auditTrail = List.copyOf(Objects.requireNonNull(auditTrail, "auditTrail is required"));
    }

    public boolean resolved() {
        return status == UsProvisionalBallotStatus.ACCEPTED || status == UsProvisionalBallotStatus.REJECTED;
    }

    public String auditSummary() {
        return "provisionalBallot=" + id + "; status=" + status + "; reason=" + reasonCode;
    }

    static String normalizeReason(String reasonCode) {
        String normalized = Objects.requireNonNull(reasonCode, "reasonCode is required").trim().toUpperCase().replace(' ', '_');
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("reasonCode is required");
        }
        return normalized;
    }
}
