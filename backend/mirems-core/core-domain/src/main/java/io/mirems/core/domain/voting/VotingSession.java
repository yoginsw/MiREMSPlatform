package io.mirems.core.domain.voting;

import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.election.Election;
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
import java.util.Objects;
import java.util.UUID;

/** Entity representing a voter interaction with a ballot style for one election. */
@Entity
@Table(name = "voting_sessions")
public class VotingSession {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voter_record_id", nullable = false)
    private VoterRecord voterRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ballot_style_id", nullable = false)
    private BallotStyle ballotStyle;

    @Column(name = "started_at", nullable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime startedAt;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "completed_at", columnDefinition = "timestamp with time zone")
    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false)
    private SessionStatus sessionStatus;

    protected VotingSession() {
        // JPA constructor.
    }

    private VotingSession(
            UUID id,
            VoterRecord voterRecord,
            Election election,
            BallotStyle ballotStyle,
            String deviceId,
            OffsetDateTime startedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.voterRecord = Objects.requireNonNull(voterRecord, "voterRecord is required");
        this.election = Objects.requireNonNull(election, "election is required");
        this.ballotStyle = Objects.requireNonNull(ballotStyle, "ballotStyle is required");
        this.deviceId = VoterRecord.requireText(deviceId, "deviceId");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt is required");
        this.sessionStatus = SessionStatus.OPENED;
    }

    static VotingSession open(
            UUID id,
            VoterRecord voterRecord,
            Election election,
            BallotStyle ballotStyle,
            String deviceId,
            OffsetDateTime startedAt) {
        return new VotingSession(id, voterRecord, election, ballotStyle, deviceId, startedAt);
    }

    public void cast(OffsetDateTime completedAt) {
        completeAs(SessionStatus.CAST, completedAt);
    }

    public void spoil(OffsetDateTime completedAt) {
        completeAs(SessionStatus.SPOILED, completedAt);
    }

    public void expire(OffsetDateTime completedAt) {
        completeAs(SessionStatus.EXPIRED, completedAt);
    }

    public UUID getId() {
        return id;
    }

    public VoterRecord getVoterRecord() {
        return voterRecord;
    }

    public Election getElection() {
        return election;
    }

    public BallotStyle getBallotStyle() {
        return ballotStyle;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }

    public String getDeviceId() {
        return deviceId;
    }

    boolean isSpoiled() {
        return sessionStatus == SessionStatus.SPOILED;
    }

    private void completeAs(SessionStatus targetStatus, OffsetDateTime completedAt) {
        sessionStatus.assertCanTransitionTo(targetStatus);
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt is required");
        this.sessionStatus = targetStatus;
    }
}
