package io.mirems.core.domain.election.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Domain event emitted when an election moves from DRAFT to PUBLISHED. */
public record ElectionPublishedEvent(UUID electionId, OffsetDateTime occurredAt) {}
