package io.mirems.core.api.dto;

import io.mirems.core.domain.election.ElectionStatus;
import io.mirems.core.domain.election.ElectionType;
import java.time.LocalDate;
import java.util.UUID;

public record ElectionResponse(
        UUID id,
        String name,
        ElectionType electionType,
        String jurisdiction,
        LocalDate scheduledDate,
        ElectionStatus electionStatus,
        String countryCode,
        String extensionPackId) {
}
