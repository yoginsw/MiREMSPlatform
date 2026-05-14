package io.mirems.core.domain.election.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Domain event emitted when an election moves from ACTIVE to CLOSED. */
public record ElectionClosedEvent(UUID electionId, OffsetDateTime occurredAt) {}
