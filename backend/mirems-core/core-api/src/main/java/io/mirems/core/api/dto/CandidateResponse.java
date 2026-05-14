package io.mirems.core.api.dto;

import io.mirems.core.domain.contest.CandidateStatus;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        UUID contestId,
        UUID electionId,
        String name,
        String partyAffiliation,
        CandidateStatus candidateStatus) {
}
