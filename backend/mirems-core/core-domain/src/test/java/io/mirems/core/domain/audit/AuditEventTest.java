package io.mirems.core.domain.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;

class AuditEventTest {
    private static final UUID EVENT_ID = UUID.fromString("018f4b7f-1010-7111-8111-111111111111");
    private static final UUID AGGREGATE_ID = UUID.fromString("018f4b7f-2020-7222-8222-222222222222");
    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-06-03T10:15:30Z");

    @Test
    void auditEventIsAppendOnlyJpaEntityWithNoSetters() throws Exception {
        assertNotNull(AuditEvent.class.getAnnotation(Immutable.class));
        for (String field : List.of("id", "eventType", "aggregateId", "aggregateType", "payload", "actorId", "occurredAt")) {
            Column column = AuditEvent.class.getDeclaredField(field).getAnnotation(Column.class);
            assertNotNull(column, field + " must have @Column");
            assertFalse(column.updatable(), field + " must not be updatable");
        }
        List<String> setters = Arrays.stream(AuditEvent.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();
        assertTrue(setters.isEmpty());
    }

    @Test
    void createAuditEventInitializesFieldsAndImmutablePayload() {
        AuditEvent event = auditEvent();

        assertEquals(EVENT_ID, event.getId());
        assertEquals("ElectionPublished", event.getEventType());
        assertEquals(AGGREGATE_ID, event.getAggregateId());
        assertEquals("Election", event.getAggregateType());
        assertEquals("admin-001", event.getActorId());
        assertEquals(OCCURRED_AT, event.getOccurredAt());
        assertEquals("127.0.0.1", event.getSourceIp());
        assertEquals("PUBLISHED", event.getPayload().get("status"));
        assertThrows(UnsupportedOperationException.class, () -> event.getPayload().put("tamper", "true"));
    }

    @Test
    void auditEventRejectsInvalidRequiredFieldsAndNormalizesOptionalSourceIp() {
        Map<String, Object> payload = Map.of("status", "PUBLISHED");

        assertThrows(NullPointerException.class,
                () -> AuditEvent.create(null, "ElectionPublished", AGGREGATE_ID, "Election", payload, "admin", OCCURRED_AT, null));
        assertThrows(IllegalArgumentException.class,
                () -> AuditEvent.create(EVENT_ID, " ", AGGREGATE_ID, "Election", payload, "admin", OCCURRED_AT, null));
        assertThrows(NullPointerException.class,
                () -> AuditEvent.create(EVENT_ID, "ElectionPublished", null, "Election", payload, "admin", OCCURRED_AT, null));
        assertThrows(IllegalArgumentException.class,
                () -> AuditEvent.create(EVENT_ID, "ElectionPublished", AGGREGATE_ID, " ", payload, "admin", OCCURRED_AT, null));
        assertThrows(NullPointerException.class,
                () -> AuditEvent.create(EVENT_ID, "ElectionPublished", AGGREGATE_ID, "Election", null, "admin", OCCURRED_AT, null));
        assertThrows(IllegalArgumentException.class,
                () -> AuditEvent.create(EVENT_ID, "ElectionPublished", AGGREGATE_ID, "Election", payload, " ", OCCURRED_AT, null));
        assertThrows(NullPointerException.class,
                () -> AuditEvent.create(EVENT_ID, "ElectionPublished", AGGREGATE_ID, "Election", payload, "admin", null, null));

        AuditEvent withoutIp = AuditEvent.create(
                EVENT_ID, "ElectionPublished", AGGREGATE_ID, "Election", payload, "admin", OCCURRED_AT, " ");
        assertEquals(null, withoutIp.getSourceIp());
    }

    private static AuditEvent auditEvent() {
        return AuditEvent.create(
                EVENT_ID,
                "ElectionPublished",
                AGGREGATE_ID,
                "Election",
                Map.of("status", "PUBLISHED"),
                "admin-001",
                OCCURRED_AT,
                " 127.0.0.1 ");
    }
}
