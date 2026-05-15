package io.mirems.core.api.security;

import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ElectionScopedAuthorizationAspect {
    private final ElectionScopeValidator electionScopeValidator;

    public ElectionScopedAuthorizationAspect(ElectionScopeValidator electionScopeValidator) {
        this.electionScopeValidator = electionScopeValidator;
    }

    @Around("@annotation(electionScoped)")
    public Object authorizeElectionScope(ProceedingJoinPoint joinPoint, ElectionScoped electionScoped) throws Throwable {
        UUID electionId = electionId(joinPoint, electionScoped.electionIdParameter());
        electionScopeValidator.requireAccess(electionId);
        return joinPoint.proceed();
    }

    private UUID electionId(ProceedingJoinPoint joinPoint, String parameterName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (parameterNames != null) {
            for (int index = 0; index < parameterNames.length; index++) {
                if (parameterName.equals(parameterNames[index]) && args[index] instanceof UUID id) {
                    return id;
                }
            }
        }
        throw new AccessDeniedException("Election scope parameter not found: " + parameterName);
    }
}
