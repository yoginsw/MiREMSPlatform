package io.mirems.core.bpmn.voter;

import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.voting.RegistrationStatus;
import java.util.Objects;

public record VoterEligibilityRequest(
        int voterAge,
        RegistrationStatus registrationStatus,
        boolean residencyVerified,
        ElectionType electionType) {
    public VoterEligibilityRequest {
        if (voterAge < 0) {
            throw new IllegalArgumentException("voterAge must not be negative");
        }
        Objects.requireNonNull(registrationStatus, "registrationStatus is required");
        Objects.requireNonNull(electionType, "electionType is required");
    }
}
