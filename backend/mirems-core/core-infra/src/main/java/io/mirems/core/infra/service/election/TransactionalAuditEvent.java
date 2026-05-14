package io.mirems.core.infra.service.election;

import java.util.Map;
import java.util.UUID;

/** Application event converted to an append-only audit event after the surrounding transaction commits. */
public record TransactionalAuditEvent(
        String eventType,
        UUID aggregateId,
        String aggregateType,
        Map<String, Object> payload,
        String actorId,
        String sourceIp) {
    public TransactionalAuditEvent {
        payload = Map.copyOf(payload);
    }
}
