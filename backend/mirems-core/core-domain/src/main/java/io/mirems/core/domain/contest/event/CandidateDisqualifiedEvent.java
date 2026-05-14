package io.mirems.core.domain.contest.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Domain event emitted when a candidate is disqualified. */
public record CandidateDisqualifiedEvent(
        UUID candidateId,
        UUID contestId,
        UUID electionId,
        List<String> reasons,
        OffsetDateTime occurredAt) {
    public CandidateDisqualifiedEvent {
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }
}
