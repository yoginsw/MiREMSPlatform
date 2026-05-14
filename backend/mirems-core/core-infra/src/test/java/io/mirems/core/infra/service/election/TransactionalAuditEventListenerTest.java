package io.mirems.core.infra.service.election;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.mirems.core.infra.audit.AuditEventPublisher;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class TransactionalAuditEventListenerTest {
    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Test
    void handlePublishesAuditEventThroughPublisher() {
        TransactionalAuditEventListener listener = new TransactionalAuditEventListener(auditEventPublisher);
        TransactionalAuditEvent event = new TransactionalAuditEvent(
                "ElectionPublished",
                UUID.fromString("018f4b7f-1111-7111-8111-111111111111"),
                "Election",
                Map.of("status", "PUBLISHED"),
                "admin-001",
                "127.0.0.1");

        listener.handle(event);

        verify(auditEventPublisher).publish(
                event.eventType(),
                event.aggregateId(),
                event.aggregateType(),
                event.payload(),
                event.actorId(),
                event.sourceIp());
    }

    @Test
    void handleIsTransactionalEventListenerAfterCommitOnly() throws NoSuchMethodException {
        Method method = TransactionalAuditEventListener.class.getDeclaredMethod("handle", TransactionalAuditEvent.class);

        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(annotation.fallbackExecution()).isFalse();
    }
}
