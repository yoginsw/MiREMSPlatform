package io.mirems.core.bpmn.audit;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record ChainOfCustodyEntry(
        UUID eventId,
        String eventType,
        String aggregateType,
        String actorId,
        OffsetDateTime occurredAt,
        Set<String> payloadKeys) {
    public ChainOfCustodyEntry {
        payloadKeys = Set.copyOf(payloadKeys);
    }
}
