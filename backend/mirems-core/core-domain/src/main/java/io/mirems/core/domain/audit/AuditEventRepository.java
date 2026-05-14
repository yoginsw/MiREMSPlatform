package io.mirems.core.domain.audit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Append-only/read-only persistence contract for audit events. */
public interface AuditEventRepository {
    AuditEvent save(AuditEvent auditEvent);

    Optional<AuditEvent> findById(UUID id);

    List<AuditEvent> findByAggregateId(UUID aggregateId);

    List<AuditEvent> findByEventType(String eventType);
}
