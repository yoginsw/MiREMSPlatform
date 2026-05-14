package io.mirems.core.bpmn.publication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ElectionValidationFailedEvent(
        UUID electionId,
        List<String> reasons,
        OffsetDateTime occurredAt) {
    public ElectionValidationFailedEvent {
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }
}
