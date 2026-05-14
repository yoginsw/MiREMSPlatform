package io.mirems.core.api.dto;

import io.mirems.core.domain.voting.SessionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VotingSessionResponse(
        UUID id,
        UUID voterRecordId,
        UUID electionId,
        UUID ballotStyleId,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        SessionStatus sessionStatus,
        String deviceId) {
}
