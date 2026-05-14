package io.mirems.core.api.dto;

import java.util.UUID;

public record BallotContestResponse(
        UUID contestId,
        int displayOrder,
        String presentationTitle) {
}
