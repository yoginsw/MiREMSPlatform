package io.mirems.core.infra.persistence.audit;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnBean(SpringDataAuditEventJpaRepository.class)
public class JpaAuditEventRepositoryAdapter implements AuditEventRepository {
    private final SpringDataAuditEventJpaRepository repository;

    public JpaAuditEventRepositoryAdapter(SpringDataAuditEventJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    @Override
    public AuditEvent save(AuditEvent auditEvent) {
        return repository.save(auditEvent);
    }

    @Override
    public Optional<AuditEvent> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<AuditEvent> findByAggregateId(UUID aggregateId) {
        return repository.findByAggregateIdOrderByOccurredAtAsc(aggregateId);
    }

    @Override
    public List<AuditEvent> findByEventType(String eventType) {
        return repository.findByEventType(eventType);
    }

    @Override
    public List<AuditEvent> findAllChronologically() {
        return repository.findAllByOrderByOccurredAtAscIdAsc();
    }
}
