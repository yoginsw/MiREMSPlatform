package io.mirems.core.api.security;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ElectionScopeValidator {
    private final MiremsSecurityContext securityContext;

    public ElectionScopeValidator(MiremsSecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    public void requireAccess(UUID electionId) {
        if (!canAccess(electionId)) {
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
