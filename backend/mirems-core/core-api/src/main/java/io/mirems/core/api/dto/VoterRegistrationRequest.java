package io.mirems.core.api.dto;

import io.mirems.core.domain.voting.RegistrationStatus;
import java.util.Set;
import java.util.UUID;

public record VoterRegistrationRequest(
        String externalVoterId,
        Set<UUID> eligibleElections,
        RegistrationStatus registrationStatus) {
}
