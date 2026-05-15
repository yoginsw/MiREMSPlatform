package io.mirems.core.api.security;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import io.mirems.core.infra.audit.InMemoryAuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditEventRecorder {
    static final String SECURITY_VIOLATION = "SECURITY_VIOLATION";
    static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
    static final UUID SECURITY_AGGREGATE_ID = new UUID(0L, 0L);

    private final ObjectProvider<AuditEventRepository> auditEventRepository;
    private final AuditEventRepository fallbackAuditEventRepository = new InMemoryAuditEventRepository();
    private final Clock clock;

    public SecurityAuditEventRecorder(ObjectProvider<AuditEventRepository> auditEventRepository, Clock clock) {
        this.auditEventRepository = auditEventRepository;
        this.clock = clock;
    }

    public void recordAuthenticationFailure(HttpServletRequest request, Exception exception) {
        Map<String, Object> payload = basePayload("AUTHENTICATION_FAILURE", request, "anonymous");
        payload.put("reason", authenticationFailureReason(exception));
        save(SECURITY_VIOLATION, SECURITY_AGGREGATE_ID, payload, "anonymous", sourceIp(request));
    }

    public void recordAuthorizationFailure(HttpServletRequest request, Authentication authentication, Exception exception) {
        String actorId = actorId(authentication);
        Map<String, Object> payload = basePayload("AUTHORIZATION_FAILURE", request, actorId);
        payload.put("reason", authorizationFailureReason(exception));
        save(SECURITY_VIOLATION, SECURITY_AGGREGATE_ID, payload, actorId, sourceIp(request));
    }

    public void recordElectionScopeViolation(UUID electionId, Authentication authentication, HttpServletRequest request) {
        String actorId = actorId(authentication);
        Map<String, Object> payload = basePayload("ELECTION_SCOPE_VIOLATION", request, actorId);
        payload.put("electionId", electionId == null ? "" : electionId.toString());
        save(SECURITY_VIOLATION, electionId == null ? SECURITY_AGGREGATE_ID : electionId, payload, actorId, sourceIp(request));
    }

    public void recordAuthenticationSuccess(HttpServletRequest request, Authentication authentication) {
        String actorId = actorId(authentication);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("actorId", actorId);
        payload.put("authenticationType", authenticationType(authentication));
        payload.put("path", request.getRequestURI());
        payload.put("method", request.getMethod());
        save(AUTHENTICATION_SUCCESS, SECURITY_AGGREGATE_ID, payload, actorId, sourceIp(request));
    }

    private Map<String, Object> basePayload(String violationType, HttpServletRequest request, String actorId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("violationType", violationType);
        payload.put("actorId", actorId);
        payload.put("path", request == null ? "" : request.getRequestURI());
        payload.put("method", request == null ? "" : request.getMethod());
        return payload;
    }

    private void save(String eventType, UUID aggregateId, Map<String, Object> payload, String actorId, String sourceIp) {
        AuditEventRepository repository = auditEventRepository.getIfAvailable(() -> fallbackAuditEventRepository);
        repository.save(AuditEvent.create(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                "Security",
                payload,
                actorId,
                OffsetDateTime.now(clock),
                sourceIp));
    }

    private static String actorId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private static String authenticationType(Authentication authentication) {
        if (authentication == null) {
            return "UNKNOWN";
        }
        return authentication.getClass().getSimpleName().contains("Jwt") ? "JWT" : authentication.getClass().getSimpleName();
    }

    private static String authenticationFailureReason(Exception exception) {
        return "AUTHENTICATION_FAILED";
    }

    private static String authorizationFailureReason(Exception exception) {
        return "AUTHORIZATION_DENIED";
    }

    private static String sourceIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getRemoteAddr();
    }
}
