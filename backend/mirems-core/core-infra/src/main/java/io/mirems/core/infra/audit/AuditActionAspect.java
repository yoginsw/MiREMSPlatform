package io.mirems.core.infra.audit;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/** AOP adapter that emits an audit event only after an annotated service method returns successfully. */
@Aspect
public class AuditActionAspect {
    private final AuditEventPublisher publisher;
    private final Clock clock;

    public AuditActionAspect(AuditEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    @Around("@annotation(auditAction)")
    public Object emitAuditEventOnSuccess(ProceedingJoinPoint joinPoint, AuditAction auditAction) throws Throwable {
        Object result = joinPoint.proceed();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        UUID aggregateId = UUID.nameUUIDFromBytes((
                        auditAction.aggregateType() + ":" + Arrays.deepToString(joinPoint.getArgs()))
                .getBytes(StandardCharsets.UTF_8));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("class", signature.getDeclaringType().getSimpleName());
        payload.put("method", signature.getMethod().getName());
        payload.put("args", Arrays.deepToString(joinPoint.getArgs()));
        payload.put("result", String.valueOf(result));
        payload.put("observedAt", clock.instant().toString());

        publisher.publish(
                auditAction.eventType(),
                aggregateId,
                auditAction.aggregateType(),
                payload,
                auditAction.actorId(),
                auditAction.sourceIp());
        return result;
    }
}
