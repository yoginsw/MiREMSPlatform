package io.mirems.core.api.voter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class VoterRollExceptionHandlerTest {
    private final VoterRollExceptionHandler handler = new VoterRollExceptionHandler();

    @Test
    void accessDeniedMapsToForbiddenProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Forbidden");
        assertThat(response.getBody().getDetail()).isEqualTo("denied");
    }

    @Test
    void serviceUnavailableMapsToServiceUnavailableProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleUnavailable(
                new VoterRollController.VoterServiceUnavailableException("service unavailable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Voter roll service unavailable");
    }

    @Test
    void invalidRequestMapsToBadRequestProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleInvalid(new IllegalArgumentException("invalid voter"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid voter request");
    }
}
