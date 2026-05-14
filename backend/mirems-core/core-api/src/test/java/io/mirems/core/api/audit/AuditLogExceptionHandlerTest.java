package io.mirems.core.api.audit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class AuditLogExceptionHandlerTest {
    private final AuditLogExceptionHandler handler = new AuditLogExceptionHandler();

    @Test
    void invalidQueryMapsToBadRequestProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleInvalid(
                new AuditLogController.InvalidAuditQueryException("bad query"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid audit query");
    }

    @Test
    void constraintViolationMapsToBadRequestProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleInvalid(new ConstraintViolationException(Set.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid audit query");
    }

    @Test
    void unavailableMapsToServiceUnavailableProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleUnavailable(
                new AuditLogController.AuditLogServiceUnavailableException("unavailable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Audit log service unavailable");
    }
}
