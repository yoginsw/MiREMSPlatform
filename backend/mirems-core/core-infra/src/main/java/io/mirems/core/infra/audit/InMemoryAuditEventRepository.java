package io.mirems.core.infra.audit;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
/** In-memory append-only audit repository used until the P1-015 JPA adapter is introduced. */
public class InMemoryAuditEventRepository implements AuditEventRepository {
    private final ConcurrentMap<UUID, AuditEvent> events = new ConcurrentHashMap<>();

    @Override
    public AuditEvent save(AuditEvent auditEvent) {
        AuditEvent previous = events.putIfAbsent(auditEvent.getId(), auditEvent);
        if (previous != null) {
            throw new DuplicateAuditEventException("Audit event already exists: " + auditEvent.getId());
        }
        return auditEvent;
    }

    @Override
    public Optional<AuditEvent> findById(UUID id) {
        return Optional.ofNullable(events.get(id));
    }

    @Override
    public List<AuditEvent> findByAggregateId(UUID aggregateId) {
        return sortedEvents().stream()
                .filter(event -> event.getAggregateId().equals(aggregateId))
                .toList();
    }

    @Override
    public List<AuditEvent> findByEventType(String eventType) {
        return sortedEvents().stream()
                .filter(event -> event.getEventType().equals(eventType))
                .toList();
    }

    @Override
    public List<AuditEvent> findAllChronologically() {
        return sortedEvents();
    }

    private List<AuditEvent> sortedEvents() {
        List<AuditEvent> snapshot = new ArrayList<>(events.values());
        snapshot.sort(Comparator.comparing(AuditEvent::getOccurredAt).thenComparing(AuditEvent::getId));
        return snapshot;
    }
}
