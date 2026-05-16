package io.mirems.core.bpmn.audit;

import java.util.UUID;

@FunctionalInterface
public interface AdvancedAuditAuthorizer {
    void requireAuditor(String actorId, UUID electionId);
}
