package io.mirems.core.domain.result;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Immutable, hash-signed draft/final tabulation report for one election. */
@Entity
@Table(name = "tabulation_reports")
@Immutable
public class TabulationReport {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "election_id", nullable = false, updatable = false)
    private UUID electionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contest_tallies", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<UUID, ContestTally> contestTallies;

    @Column(name = "generated_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime generatedAt;

    @Column(name = "locked_at", updatable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime lockedAt;

    @Column(name = "hash", updatable = false, length = 64)
    private String hash;

    @Column(name = "published", nullable = false, updatable = false)
    private boolean published;

    protected TabulationReport() {
        // JPA constructor.
    }

    private TabulationReport(
            UUID id,
            UUID electionId,
            Map<UUID, ContestTally> contestTallies,
            OffsetDateTime generatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.electionId = Objects.requireNonNull(electionId, "electionId is required");
        this.contestTallies = validateTallies(contestTallies);
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt is required");
        this.published = false;
    }

    public static TabulationReport draft(
            UUID id,
            UUID electionId,
            Map<UUID, ContestTally> contestTallies,
            OffsetDateTime generatedAt) {
        return new TabulationReport(id, electionId, contestTallies, generatedAt);
    }

    public TabulationCompletedEvent lock(OffsetDateTime completedAt) {
        if (!isLocked()) {
            this.lockedAt = Objects.requireNonNull(completedAt, "completedAt is required");
            this.hash = computeHash();
        }
        return new TabulationCompletedEvent(id, electionId, hash, lockedAt);
    }

    public void markPublished() {
        if (!isLocked()) {
            throw new IllegalStateException("TabulationReport must be locked before publishing");
        }
        this.published = true;
    }

    @PrePersist
    public void computeHashBeforePersist() {
        if (!isLocked()) {
            lock(generatedAt);
        }
    }

    public boolean verifyHash() {
        return hash != null && hash.equals(computeHash());
    }

    public UUID getId() {
        return id;
    }

    public UUID getElectionId() {
        return electionId;
    }

    public Map<UUID, ContestTally> getContestTallies() {
        return Map.copyOf(contestTallies);
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public OffsetDateTime getLockedAt() {
        return lockedAt;
    }

    public String getHash() {
        return hash;
    }

    public boolean isLocked() {
        return lockedAt != null && hash != null;
    }

    public boolean isPublished() {
        return published;
    }

    public int totalBallotsCounted() {
        return contestTallies.values().stream()
                .mapToInt(ContestTally::ballotsCounted)
                .sum();
    }

    private String computeHash() {
        String canonical = String.join(
                "|",
                id.toString(),
                electionId.toString(),
                canonicalTallies(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(generatedAt),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(lockedAt));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest algorithm is unavailable", exception);
        }
    }

    private String canonicalTallies() {
        return contestTallies.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + canonicalCandidateTallies(entry.getValue()))
                .collect(Collectors.joining(";"));
    }

    private String canonicalCandidateTallies(ContestTally tally) {
        return tally.candidateTallies().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(",")) + ":ballots=" + tally.ballotsCounted();
    }

    private static Map<UUID, ContestTally> validateTallies(Map<UUID, ContestTally> inputTallies) {
        Map<UUID, ContestTally> tallies = Map.copyOf(Objects.requireNonNull(inputTallies, "contestTallies is required"));
        if (tallies.isEmpty()) {
            throw new IllegalArgumentException("contestTallies must not be empty");
        }
        tallies.forEach((contestId, tally) -> {
            Objects.requireNonNull(contestId, "contestId is required");
            Objects.requireNonNull(tally, "contestTally is required");
            if (!contestId.equals(tally.contestId())) {
                throw new IllegalArgumentException("contestTally contestId must match map key");
            }
        });
        return tallies;
    }
}
