package io.mirems.core.bpmn.audit;

import io.mirems.core.domain.audit.AuditEvent;
import java.util.List;

public interface AuditReportGenerator {
    AuditReport generate(AuditReviewRequest request, List<AuditEvent> auditEvents);
}
