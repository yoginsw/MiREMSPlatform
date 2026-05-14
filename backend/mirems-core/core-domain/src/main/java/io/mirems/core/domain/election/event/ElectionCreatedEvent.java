package io.mirems.core.domain.election.event;

import io.mirems.core.domain.election.ElectionType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Domain event emitted when an election aggregate is created. */
public record ElectionCreatedEvent(
        UUID electionId,
        String name,
        ElectionType electionType,
        String jurisdiction,
        LocalDate scheduledDate,
        String countryCode,
        String extensionPackId,
        OffsetDateTime occurredAt) {}
