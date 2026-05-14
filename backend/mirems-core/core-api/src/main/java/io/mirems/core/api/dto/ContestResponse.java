package io.mirems.core.api.dto;

import io.mirems.core.domain.contest.ContestType;
import java.util.UUID;

public record ContestResponse(
        UUID id,
        UUID electionId,
        ContestType contestType,
        String name,
        int seats,
        int voteLimit) {
}
