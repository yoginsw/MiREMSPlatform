package io.mirems.core.api.dto;

import java.util.UUID;

public record CandidateCreateRequest(
        UUID contestId,
        String name,
        String partyAffiliation) {
}
