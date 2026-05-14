package io.mirems.core.api.dto;

import java.util.List;
import java.util.UUID;

public record CastBallotRequest(
        UUID sessionId,
        UUID contestId,
        List<UUID> selectedCandidateIds) {
}
