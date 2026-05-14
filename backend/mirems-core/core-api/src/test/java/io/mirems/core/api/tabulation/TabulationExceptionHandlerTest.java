package io.mirems.core.api.tabulation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

class TabulationExceptionHandlerTest {
    private final TabulationExceptionHandler handler = new TabulationExceptionHandler();

    @Test
    void electionNotFoundMapsToNotFoundProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleElectionNotFound(new EntityNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Election not found");
    }

    @Test
    void reportNotFoundMapsToNotFoundProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleReportNotFound(
                new TabulationController.TabulationReportNotFoundException("missing report"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Tabulation report not found");
    }

    @Test
    void authenticationRequiredMapsToUnauthorizedProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleAuthenticationRequired(
                new AuthenticationCredentialsNotFoundException("required"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Authentication required");
    }

    @Test
    void accessDeniedMapsToForbiddenProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Forbidden");
    }

    @Test
    void invalidRequestMapsToBadRequestProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleInvalid(new IllegalStateException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid tabulation request");
    }

    @Test
    void unavailableMapsToServiceUnavailableProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleUnavailable(
                new TabulationController.TabulationServiceUnavailableException("unavailable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Tabulation service unavailable");
    }
}
