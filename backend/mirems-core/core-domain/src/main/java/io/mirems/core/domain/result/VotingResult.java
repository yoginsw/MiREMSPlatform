package io.mirems.core.domain.result;

import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.voting.VotingSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only immutable vote record with deterministic tamper-evident hash. */
@Entity
@Table(name = "voting_results")
@Immutable
public class VotingResult {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private VotingSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id", nullable = false, updatable = false)
    private Contest contest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_candidate_ids", nullable = false, updatable = false, columnDefinition = "jsonb")
    private List<UUID> selectedCandidateIds;

    @Column(name = "cast_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime castAt;

    @Column(name = "hash", nullable = false, updatable = false, length = 64)
    private String hash;

    protected VotingResult() {
        // JPA constructor.
    }

    private VotingResult(
            UUID id,
            VotingSession session,
            Contest contest,
            List<UUID> selectedCandidateIds,
            OffsetDateTime castAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.session = Objects.requireNonNull(session, "session is required");
        this.contest = Objects.requireNonNull(contest, "contest is required");
        this.selectedCandidateIds = validateCandidateIds(selectedCandidateIds);
        this.castAt = Objects.requireNonNull(castAt, "castAt is required");
    }

    public static VotingResult create(
            UUID id,
            VotingSession session,
            Contest contest,
            List<UUID> selectedCandidateIds,
            OffsetDateTime castAt) {
        return new VotingResult(id, session, contest, selectedCandidateIds, castAt);
    }

    @PrePersist
    public void computeHashBeforePersist() {
        this.hash = computeHash();
    }

    public boolean verifyHash() {
        return hash != null && hash.equals(computeHash());
    }

    public UUID getId() {
        return id;
    }

    public VotingSession getSession() {
        return session;
    }

    public Contest getContest() {
        return contest;
    }

    public List<UUID> getSelectedCandidateIds() {
        return List.copyOf(selectedCandidateIds);
    }

    public OffsetDateTime getCastAt() {
        return castAt;
    }

    public String getHash() {
        return hash;
    }

    private String computeHash() {
        String canonical = String.join(
                "|",
                id.toString(),
                session.getId().toString(),
                contest.getId().toString(),
                selectedCandidateIds.stream().map(UUID::toString).collect(Collectors.joining(",")),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(castAt));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest algorithm is unavailable", exception);
        }
    }

    private static List<UUID> validateCandidateIds(List<UUID> candidateIds) {
        List<UUID> candidates = List.copyOf(Objects.requireNonNull(candidateIds, "selectedCandidateIds is required"));
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("selectedCandidateIds must contain at least one candidate");
        }
        return candidates;
    }
}
