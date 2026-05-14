package io.mirems.core.bpmn.process;

import java.time.OffsetDateTime;
import java.util.Map;

public record ProcessAuditEntry(
        String instanceId,
        String processId,
        String eventType,
        String actorId,
        OffsetDateTime occurredAt,
        Map<String, Object> payload) {
    public ProcessAuditEntry {
        requireText(instanceId, "instanceId");
        requireText(processId, "processId");
        requireText(eventType, "eventType");
        requireText(actorId, "actorId");
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        payload = Map.copyOf(payload == null ? Map.of() : payload);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
