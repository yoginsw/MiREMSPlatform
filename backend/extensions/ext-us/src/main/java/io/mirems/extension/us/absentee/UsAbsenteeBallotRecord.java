package io.mirems.extension.us.absentee;

import io.mirems.extension.us.rules.UsAbsenteeVoterCategory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record UsAbsenteeBallotRecord(
        UUID id,
        UUID voterId,
        UUID electionId,
        UsAbsenteeVoterCategory voterCategory,
        UsAbsenteeBallotStatus status,
        boolean uocava,
        boolean federalWriteInAbsenteeBallotAllowed,
        OffsetDateTime requestedAt,
        Optional<OffsetDateTime> sentAt,
        Optional<String> deliveryMethod,
        Optional<OffsetDateTime> returnedAt,
        Optional<OffsetDateTime> adjudicatedAt,
        List<String> auditTrail) {
    public UsAbsenteeBallotRecord {
        id = Objects.requireNonNull(id, "id is required");
        voterId = Objects.requireNonNull(voterId, "voterId is required");
        electionId = Objects.requireNonNull(electionId, "electionId is required");
        voterCategory = Objects.requireNonNull(voterCategory, "voterCategory is required");
        status = Objects.requireNonNull(status, "status is required");
        requestedAt = Objects.requireNonNull(requestedAt, "requestedAt is required");
        sentAt = Objects.requireNonNull(sentAt, "sentAt is required");
        deliveryMethod = Objects.requireNonNull(deliveryMethod, "deliveryMethod is required").map(String::trim).filter(value -> !value.isEmpty());
        returnedAt = Objects.requireNonNull(returnedAt, "returnedAt is required");
        adjudicatedAt = Objects.requireNonNull(adjudicatedAt, "adjudicatedAt is required");
        auditTrail = List.copyOf(Objects.requireNonNull(auditTrail, "auditTrail is required"));
    }

    public String auditSummary() {
        return "absenteeBallot=" + id + "; status=" + status + "; uocava=" + uocava + "; fwab=" + federalWriteInAbsenteeBallotAllowed;
    }
}
