package io.mirems.core.bpmn.audit;

import java.util.List;

public record VvsgAuditTrailVerification(boolean complete, List<String> missingEventTypes) {
    public VvsgAuditTrailVerification {
        missingEventTypes = List.copyOf(missingEventTypes);
    }
}
