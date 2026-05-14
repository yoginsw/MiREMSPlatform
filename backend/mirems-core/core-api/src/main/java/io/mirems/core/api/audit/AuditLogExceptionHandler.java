package io.mirems.core.api.audit;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(assignableTypes = AuditLogController.class)
class AuditLogExceptionHandler {
    private static final URI TYPE = URI.create("https://mirems.io/problems/audit-log-api");

    @ExceptionHandler({
        AuditLogController.InvalidAuditQueryException.class,
        ConstraintViolationException.class,
        HandlerMethodValidationException.class,
        MethodArgumentNotValidException.class
    })
    ResponseEntity<ProblemDetail> handleInvalid(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(TYPE);
        problem.setTitle("Invalid audit query");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setType(TYPE);
        problem.setTitle("Forbidden");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(AuditLogController.AuditLogServiceUnavailableException.class)
    ResponseEntity<ProblemDetail> handleUnavailable(AuditLogController.AuditLogServiceUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setType(TYPE);
        problem.setTitle("Audit log service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }
}
