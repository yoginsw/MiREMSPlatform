package io.mirems.core.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only audit event record for domain and service-level state changes. */
@Entity
@Table(name = "audit_events")
@Immutable
public class AuditEvent {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false, updatable = false)
    private String aggregateType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private String actorId;

    @Column(name = "occurred_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private OffsetDateTime occurredAt;

    @Column(name = "source_ip", updatable = false)
    private String sourceIp;

    protected AuditEvent() {
        // JPA constructor.
    }

    private AuditEvent(
            UUID id,
            String eventType,
            UUID aggregateId,
            String aggregateType,
            Map<String, Object> payload,
            String actorId,
            OffsetDateTime occurredAt,
            String sourceIp) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.eventType = requireText(eventType, "eventType");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId is required");
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.payload = Map.copyOf(Objects.requireNonNull(payload, "payload is required"));
        this.actorId = requireText(actorId, "actorId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.sourceIp = sourceIp == null || sourceIp.isBlank() ? null : sourceIp.strip();
    }

    public static AuditEvent create(
            UUID id,
            String eventType,
            UUID aggregateId,
            String aggregateType,
            Map<String, Object> payload,
            String actorId,
            OffsetDateTime occurredAt,
            String sourceIp) {
        return new AuditEvent(id, eventType, aggregateId, aggregateType, payload, actorId, occurredAt, sourceIp);
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public Map<String, Object> getPayload() {
        return Map.copyOf(payload);
    }

    public String getActorId() {
        return actorId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
