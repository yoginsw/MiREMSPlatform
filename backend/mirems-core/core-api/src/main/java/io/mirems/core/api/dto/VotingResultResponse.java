package io.mirems.core.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VotingResultResponse(
        UUID id,
        UUID sessionId,
        UUID contestId,
        List<UUID> selectedCandidateIds,
        OffsetDateTime castAt,
        String hash) {
}
