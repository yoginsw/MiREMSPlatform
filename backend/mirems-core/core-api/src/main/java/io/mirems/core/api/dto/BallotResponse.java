package io.mirems.core.api.dto;

import java.util.List;
import java.util.UUID;

public record BallotResponse(
        UUID id,
        UUID electionId,
        int ballotVersion,
        boolean active,
        List<BallotContestResponse> contests,
        List<BallotStyleResponse> styles) {
}
