package io.mirems.core.bpmn.audit;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({AuditEventRepository.class, AuditReportGenerator.class})
public class AuditReviewProcessService {
    private final AuditEventRepository auditEventRepository;
    private final AuditReportGenerator auditReportGenerator;

    public AuditReviewProcessService(AuditEventRepository auditEventRepository, AuditReportGenerator auditReportGenerator) {
        this.auditEventRepository = Objects.requireNonNull(auditEventRepository, "auditEventRepository is required");
        this.auditReportGenerator = Objects.requireNonNull(auditReportGenerator, "auditReportGenerator is required");
    }

    public AuditReviewProcessResult generate(AuditReviewRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (!"AUDITOR".equals(request.initiatorRole())) {
            throw new IllegalArgumentException("AUDITOR role is required to initiate audit review");
        }
        List<AuditEvent> events = auditEventRepository.findByAggregateId(request.electionId()).stream()
                .sorted(Comparator.comparing(AuditEvent::getOccurredAt).thenComparing(AuditEvent::getId))
                .toList();
        return new AuditReviewProcessResult(auditReportGenerator.generate(request, events));
    }
}
