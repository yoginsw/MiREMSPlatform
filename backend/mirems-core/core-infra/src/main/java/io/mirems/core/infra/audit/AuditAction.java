package io.mirems.core.infra.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a successful service method invocation as an auditable action. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {
    String eventType();

    String aggregateType();

    String actorId() default "system";

    String sourceIp() default "";
}
