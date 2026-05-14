package io.mirems.core.infra.service.election;

import io.mirems.core.infra.audit.AuditEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Bridges transactional service events to append-only audit persistence after commit. */
@Component
@ConditionalOnBean(AuditEventPublisher.class)
public class TransactionalAuditEventListener {
    private final AuditEventPublisher auditEventPublisher;

    public TransactionalAuditEventListener(AuditEventPublisher auditEventPublisher) {
        this.auditEventPublisher = auditEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TransactionalAuditEvent event) {
        auditEventPublisher.publish(
                event.eventType(),
                event.aggregateId(),
                event.aggregateType(),
                event.payload(),
                event.actorId(),
                event.sourceIp());
    }
}
