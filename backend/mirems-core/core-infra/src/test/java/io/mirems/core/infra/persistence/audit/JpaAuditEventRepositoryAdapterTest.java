package io.mirems.core.infra.persistence.audit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.domain.audit.AuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JpaAuditEventRepositoryAdapterTest {
    @Test
    void delegatesDomainRepositoryReadsAndAppendToSpringDataRepository() {
        SpringDataAuditEventJpaRepository repository = mock(SpringDataAuditEventJpaRepository.class);
        JpaAuditEventRepositoryAdapter adapter = new JpaAuditEventRepositoryAdapter(repository);
        AuditEvent event = mock(AuditEvent.class);
        UUID eventId = UUID.fromString("018f4bd0-1111-7111-8111-111111111111");
        UUID electionId = UUID.fromString("018f4bd0-2222-7222-8222-222222222222");
        when(repository.save(event)).thenReturn(event);
        when(repository.findById(eventId)).thenReturn(Optional.of(event));
        when(repository.findByAggregateIdOrderByOccurredAtAsc(electionId)).thenReturn(List.of(event));
        when(repository.findByEventType("ElectionClosed")).thenReturn(List.of(event));

        assertSame(event, adapter.save(event));
        assertTrue(adapter.findById(eventId).isPresent());
        assertSame(event, adapter.findByAggregateId(electionId).getFirst());
        assertSame(event, adapter.findByEventType("ElectionClosed").getFirst());

        verify(repository).save(event);
        verify(repository).findById(eventId);
        verify(repository).findByAggregateIdOrderByOccurredAtAsc(electionId);
        verify(repository).findByEventType("ElectionClosed");
    }
}
