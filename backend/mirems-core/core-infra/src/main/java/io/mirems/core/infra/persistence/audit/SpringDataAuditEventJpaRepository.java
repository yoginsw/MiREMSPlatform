package io.mirems.core.infra.persistence.audit;

import io.mirems.core.domain.audit.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAuditEventJpaRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByAggregateIdOrderByOccurredAtAsc(UUID aggregateId);

    List<AuditEvent> findByEventType(String eventType);

    List<AuditEvent> findAllByOrderByOccurredAtAscIdAsc();
}
