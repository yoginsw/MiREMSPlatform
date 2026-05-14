package io.mirems.core.domain.voting;

import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.voting.encryption.Encrypted;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entity representing a voter eligible for one or more elections. */
@Entity
@Table(name = "voter_records")
public class VoterRecord {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Encrypted
    @Column(name = "encrypted_external_voter_id", nullable = false)
    private String encryptedExternalVoterId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "eligible_elections", nullable = false, columnDefinition = "jsonb")
    private Set<UUID> eligibleElections;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false)
    private RegistrationStatus registrationStatus;

    @OneToMany(mappedBy = "voterRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<VotingSession> votingSessions = new ArrayList<>();

    protected VoterRecord() {
        // JPA constructor.
    }

    private VoterRecord(
            UUID id,
            String externalVoterId,
            Set<UUID> eligibleElections,
            RegistrationStatus registrationStatus,
            PiiEncryptionService encryptionService) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.encryptedExternalVoterId = Objects.requireNonNull(encryptionService, "encryptionService is required")
                .encrypt(requireText(externalVoterId, "externalVoterId"));
        this.eligibleElections = validateEligibleElections(eligibleElections);
        this.registrationStatus = Objects.requireNonNull(registrationStatus, "registrationStatus is required");
    }

    public static VoterRecord create(
            UUID id,
            String externalVoterId,
            Set<UUID> eligibleElections,
            RegistrationStatus registrationStatus,
            PiiEncryptionService encryptionService) {
        return new VoterRecord(id, externalVoterId, eligibleElections, registrationStatus, encryptionService);
    }

    public VotingSession openVotingSession(
            UUID id,
            Election election,
            BallotStyle ballotStyle,
            String deviceId,
            OffsetDateTime startedAt) {
        Election targetElection = Objects.requireNonNull(election, "election is required");
        ensureEligibleFor(targetElection);
        ensureNoNonSpoiledSessionFor(targetElection);
        VotingSession votingSession = VotingSession.open(id, this, targetElection, ballotStyle, deviceId, startedAt);
        votingSessions.add(votingSession);
        return votingSession;
    }

    public UUID getId() {
        return id;
    }

    public String getEncryptedExternalVoterId() {
        return encryptedExternalVoterId;
    }

    public String decryptExternalVoterId(PiiEncryptionService encryptionService) {
        return Objects.requireNonNull(encryptionService, "encryptionService is required").decrypt(encryptedExternalVoterId);
    }

    public Set<UUID> getEligibleElections() {
        return eligibleElections;
    }

    public void updateEligibility(Set<UUID> eligibleElections) {
        this.eligibleElections = validateEligibleElections(eligibleElections);
    }

    public boolean isEligibleFor(UUID electionId) {
        return registrationStatus == RegistrationStatus.ACTIVE
                && eligibleElections.contains(Objects.requireNonNull(electionId, "electionId is required"));
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public List<VotingSession> getVotingSessions() {
        return List.copyOf(votingSessions);
    }

    private void ensureEligibleFor(Election election) {
        if (!eligibleElections.contains(election.getId())) {
            throw new VotingSessionValidationException("voter is not eligible for election " + election.getId());
        }
    }

    private void ensureNoNonSpoiledSessionFor(Election election) {
        boolean hasNonSpoiledSession = votingSessions.stream()
                .anyMatch(session -> session.getElection().getId().equals(election.getId()) && !session.isSpoiled());
        if (hasNonSpoiledSession) {
            throw new VotingSessionValidationException(
                    "A voter may have at most one non-SPOILED VotingSession per election");
        }
    }

    private static Set<UUID> validateEligibleElections(Set<UUID> eligibleElections) {
        Set<UUID> elections = Set.copyOf(Objects.requireNonNull(eligibleElections, "eligibleElections is required"));
        if (elections.isEmpty()) {
            throw new IllegalArgumentException("eligibleElections must contain at least one election");
        }
        return elections;
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
