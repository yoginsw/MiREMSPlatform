package io.mirems.core.infra.audit;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.kafka.core.KafkaTemplate;

/** Persists audit events and publishes the same append-only record to Kafka. */
public class AuditEventPublisher {
    public static final String AUDIT_TOPIC = "mirems.audit.events";

    private final AuditEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public AuditEventPublisher(AuditEventRepository repository, KafkaTemplate<String, String> kafkaTemplate, Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public AuditEvent publish(
            String eventType,
            UUID aggregateId,
            String aggregateType,
            Map<String, Object> payload,
            String actorId,
            String sourceIp) {
        AuditEvent event = AuditEvent.create(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                aggregateType,
                payload,
                actorId,
                OffsetDateTime.now(clock),
                sourceIp);
        return publish(event);
    }

    public AuditEvent publish(AuditEvent event) {
        AuditEvent saved = repository.save(event);
        kafkaTemplate.send(AUDIT_TOPIC, saved.getId().toString(), toJson(saved)).join();
        return saved;
    }

    private String toJson(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "id", event.getId().toString(),
                    "eventType", event.getEventType(),
                    "aggregateId", event.getAggregateId().toString(),
                    "aggregateType", event.getAggregateType(),
                    "payload", event.getPayload(),
                    "actorId", event.getActorId(),
                    "occurredAt", event.getOccurredAt().toString(),
                    "sourceIp", event.getSourceIp() == null ? "" : event.getSourceIp()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize audit event: " + event.getId(), exception);
        }
    }
}
