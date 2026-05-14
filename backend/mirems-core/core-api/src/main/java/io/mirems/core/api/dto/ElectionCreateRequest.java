package io.mirems.core.api.dto;

import io.mirems.core.domain.election.ElectionType;
import java.time.LocalDate;

public record ElectionCreateRequest(
        String name,
        ElectionType electionType,
        String jurisdiction,
        LocalDate scheduledDate,
        String countryCode,
        String extensionPackId) {
}
