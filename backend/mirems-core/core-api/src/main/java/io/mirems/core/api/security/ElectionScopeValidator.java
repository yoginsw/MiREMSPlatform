package io.mirems.core.api.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ElectionScopeValidator {
    private final MiremsSecurityContext securityContext;
    private final SecurityAuditEventRecorder securityAuditEventRecorder;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    public ElectionScopeValidator(
            MiremsSecurityContext securityContext,
            SecurityAuditEventRecorder securityAuditEventRecorder,
            ObjectProvider<HttpServletRequest> requestProvider) {
        this.securityContext = securityContext;
        this.securityAuditEventRecorder = securityAuditEventRecorder;
        this.requestProvider = requestProvider;
    }

    public void requireAccess(UUID electionId) {
        if (!canAccess(electionId)) {
            securityAuditEventRecorder.recordElectionScopeViolation(
                    electionId, SecurityContextHolder.getContext().getAuthentication(), requestProvider.getIfAvailable());
            throw new AccessDeniedException("Election scope access denied: " + electionId);
        }
    }

    public boolean canAccess(UUID electionId) {
        if (electionId == null) {
            return false;
        }
        return securityContext.electionScope().stream()
                .anyMatch(scope -> "*".equals(scope) || electionId.toString().equals(scope));
    }
}
