package io.mirems.core.api.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.HttpStatusAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;

@Component
public class SecurityAuditingAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityAuditEventRecorder recorder;
    private final AccessDeniedHandler delegate = new HttpStatusAccessDeniedHandler(HttpStatus.FORBIDDEN);

    public SecurityAuditingAccessDeniedHandler(SecurityAuditEventRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isElectionScopeViolation(accessDeniedException)) {
            recorder.recordAuthorizationFailure(request, authentication, accessDeniedException);
        }
        delegate.handle(request, response, accessDeniedException);
    }

    private boolean isElectionScopeViolation(AccessDeniedException accessDeniedException) {
        return accessDeniedException.getMessage() != null
                && accessDeniedException.getMessage().startsWith("Election scope access denied:");
    }
}
