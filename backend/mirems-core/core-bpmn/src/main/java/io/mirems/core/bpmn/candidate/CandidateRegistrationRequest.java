package io.mirems.core.bpmn.candidate;

import io.mirems.core.domain.contest.Candidate;
import java.time.OffsetDateTime;

public record CandidateRegistrationRequest(
        Candidate candidate,
        int candidateAge,
        boolean residencyVerified,
        String reviewerRole,
        CandidateOfficerDecision officerDecision,
        OffsetDateTime submittedAt) {
}
