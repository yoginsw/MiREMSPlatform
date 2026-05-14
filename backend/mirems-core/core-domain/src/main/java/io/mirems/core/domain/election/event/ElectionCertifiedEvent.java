package io.mirems.core.domain.election.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Domain event emitted when an election moves from CLOSED to CERTIFIED. */
public record ElectionCertifiedEvent(UUID electionId, OffsetDateTime occurredAt) {}
