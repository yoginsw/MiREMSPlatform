package io.mirems.core.domain.contest.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Domain event emitted when a candidate is withdrawn. */
public record CandidateWithdrawnEvent(
        UUID candidateId,
        UUID contestId,
        UUID electionId,
        OffsetDateTime occurredAt) {}
