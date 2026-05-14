package io.mirems.core.domain.result;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only amendment request that references, but never mutates, the original vote record. */
@Entity
@Table(name = "vote_corrections")
@Immutable
public class VoteCorrection {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_voting_result_id", nullable = false, updatable = false)
    private VotingResult originalVotingResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "corrected_candidate_ids", nullable = false, updatable = false, columnDefinition = "jsonb")
    private List<UUID> correctedCandidateIds;

    @Column(name = "reason", nullable = false, updatable = false, length = 500)
    private String reason;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime requestedAt;

    @Column(name = "first_approved_by", updatable = false)
    private String firstApprovedBy;

    @Column(name = "first_approved_at", updatable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime firstApprovedAt;

    @Column(name = "second_approved_by", updatable = false)
    private String secondApprovedBy;

    @Column(name = "second_approved_at", updatable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime secondApprovedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "correction_status", nullable = false, updatable = false)
    private CorrectionStatus correctionStatus;

    protected VoteCorrection() {
        // JPA constructor.
    }

    private VoteCorrection(
            UUID id,
            VotingResult originalVotingResult,
            List<UUID> correctedCandidateIds,
            String reason,
            String requestedBy,
            OffsetDateTime requestedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.originalVotingResult = Objects.requireNonNull(originalVotingResult, "originalVotingResult is required");
        this.correctedCandidateIds = validateCandidateIds(correctedCandidateIds);
        this.reason = requireText(reason, "reason");
        this.requestedBy = requireText(requestedBy, "requestedBy");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt is required");
        this.correctionStatus = CorrectionStatus.PENDING_APPROVAL;
    }

    public static VoteCorrection request(
            UUID id,
            VotingResult originalVotingResult,
            List<UUID> correctedCandidateIds,
            String reason,
            String requestedBy,
            OffsetDateTime requestedAt) {
        return new VoteCorrection(id, originalVotingResult, correctedCandidateIds, reason, requestedBy, requestedAt);
    }

    public void recordFirstApproval(String approver, OffsetDateTime approvedAt) {
        ensurePendingApproval();
        this.firstApprovedBy = requireText(approver, "firstApprovedBy");
        this.firstApprovedAt = Objects.requireNonNull(approvedAt, "firstApprovedAt is required");
        this.correctionStatus = CorrectionStatus.FIRST_APPROVED;
    }

    public VoteCorrectedEvent recordSecondApproval(String approver, OffsetDateTime approvedAt) {
        ensureFirstApproved();
        String secondApprover = requireText(approver, "secondApprovedBy");
        if (secondApprover.equals(firstApprovedBy)) {
            throw new IllegalArgumentException("second approver must be different from first approver");
        }
        this.secondApprovedBy = secondApprover;
        this.secondApprovedAt = Objects.requireNonNull(approvedAt, "secondApprovedAt is required");
        this.correctionStatus = CorrectionStatus.APPROVED;
        return new VoteCorrectedEvent(
                id,
                originalVotingResult.getId(),
                originalVotingResult.getSelectedCandidateIds(),
                correctedCandidateIds,
                reason,
                requestedBy,
                requestedAt,
                firstApprovedBy,
                firstApprovedAt,
                secondApprovedBy,
                secondApprovedAt);
    }

    public UUID getId() {
        return id;
    }

    public VotingResult getOriginalVotingResult() {
        return originalVotingResult;
    }

    public List<UUID> getCorrectedCandidateIds() {
        return List.copyOf(correctedCandidateIds);
    }

    public String getReason() {
        return reason;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getFirstApprovedBy() {
        return firstApprovedBy;
    }

    public OffsetDateTime getFirstApprovedAt() {
        return firstApprovedAt;
    }

    public String getSecondApprovedBy() {
        return secondApprovedBy;
    }

    public OffsetDateTime getSecondApprovedAt() {
        return secondApprovedAt;
    }

    public CorrectionStatus getCorrectionStatus() {
        return correctionStatus;
    }

    private void ensurePendingApproval() {
        if (correctionStatus != CorrectionStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("correction is not pending approval");
        }
    }

    private void ensureFirstApproved() {
        if (correctionStatus != CorrectionStatus.FIRST_APPROVED) {
            throw new IllegalStateException("correction requires first approval before second approval");
        }
    }

    private static List<UUID> validateCandidateIds(List<UUID> candidateIds) {
        List<UUID> candidates = List.copyOf(Objects.requireNonNull(candidateIds, "correctedCandidateIds is required"));
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("correctedCandidateIds must contain at least one candidate");
        }
        return candidates;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
