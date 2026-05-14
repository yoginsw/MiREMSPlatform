package io.mirems.core.api.dto;

import java.util.UUID;

public record VotingSessionOpenRequest(
        UUID voterRecordId,
        UUID electionId,
        UUID ballotStyleId,
        String deviceId) {
}
