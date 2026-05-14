package io.mirems.core.domain.voting;

import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.election.Election;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** Entity representing a voter interaction with a ballot style for one election. */
public class VotingSession {
    private final UUID id;
    private final VoterRecord voterRecord;
    private final Election election;
    private final BallotStyle ballotStyle;
    private final OffsetDateTime startedAt;
    private final String deviceId;

    private OffsetDateTime completedAt;
    private SessionStatus sessionStatus;

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
