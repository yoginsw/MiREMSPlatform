package io.mirems.core.api.audit;

import io.mirems.core.api.generated.api.AuditApi;
import io.mirems.core.api.generated.model.AuditLogEntry;
import io.mirems.core.api.generated.model.AuditLogPageResponse;
import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
public class AuditLogController implements AuditApi {
    private final ObjectProvider<AuditEventRepository> auditEventRepository;

    public AuditLogController(ObjectProvider<AuditEventRepository> auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @PreAuthorize("hasAnyRole('AUDITOR','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<AuditLogPageResponse> searchAuditEvents(
            UUID aggregateId,
            String aggregateType,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer page,
            Integer size) {
        validateQuery(from, to, page, size);

        List<AuditEvent> filtered = repository().findAllChronologically().stream()
                .filter(event -> aggregateId == null || event.getAggregateId().equals(aggregateId))
                .filter(event -> aggregateType == null || event.getAggregateType().equals(aggregateType.strip()))
                .filter(event -> from == null || !event.getOccurredAt().isBefore(from))
                .filter(event -> to == null || !event.getOccurredAt().isAfter(to))
                .sorted(Comparator.comparing(AuditEvent::getOccurredAt).thenComparing(AuditEvent::getId))
                .toList();

        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? 20 : size;
        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        List<AuditLogEntry> content = filtered.subList(fromIndex, toIndex).stream()
                .map(this::toEntry)
                .toList();
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / safeSize);

        return ResponseEntity.ok(new AuditLogPageResponse(content, safePage, safeSize, (long) filtered.size(), totalPages));
    }

    private void validateQuery(OffsetDateTime from, OffsetDateTime to, Integer page, Integer size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidAuditQueryException("from must be before or equal to to");
        }
        if (page != null && page < 0) {
            throw new InvalidAuditQueryException("page must be greater than or equal to 0");
        }
        if (size != null && (size < 1 || size > 100)) {
            throw new InvalidAuditQueryException("size must be between 1 and 100");
        }
    }

    private AuditLogEntry toEntry(AuditEvent event) {
        return new AuditLogEntry(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateId(),
                        event.getAggregateType(),
                        event.getActorId(),
                        event.getOccurredAt(),
                        Map.copyOf(event.getPayload()))
                .sourceIp(event.getSourceIp());
    }

    private AuditEventRepository repository() {
        AuditEventRepository repository = auditEventRepository.getIfAvailable();
        if (repository == null) {
            throw new AuditLogServiceUnavailableException("Audit event repository is unavailable");
        }
        return repository;
    }

    static final class InvalidAuditQueryException extends RuntimeException {
        InvalidAuditQueryException(String message) {
            super(message);
        }
    }

    static final class AuditLogServiceUnavailableException extends RuntimeException {
        AuditLogServiceUnavailableException(String message) {
            super(message);
        }
    }
}
