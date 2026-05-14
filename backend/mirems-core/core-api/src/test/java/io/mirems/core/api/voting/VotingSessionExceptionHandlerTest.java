package io.mirems.core.api.voting;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.domain.voting.VotingSessionValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class VotingSessionExceptionHandlerTest {
    private final VotingSessionExceptionHandler handler = new VotingSessionExceptionHandler();

    @Test
    void validationExceptionMapsToConflictProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleConflict(new VotingSessionValidationException("duplicate"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Voting session conflict");
    }

    @Test
    void notFoundMapsToNotFoundProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleNotFound(new EntityNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Voting session not found");
    }

    @Test
    void accessDeniedMapsToForbiddenProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Forbidden");
    }

    @Test
    void unavailableMapsToServiceUnavailableProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleUnavailable(
                new VotingSessionController.VotingSessionServiceUnavailableException("unavailable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Voting session service unavailable");
    }

    @Test
    void badRequestMapsToBadRequestProblemDetail() {
        ResponseEntity<ProblemDetail> response = handler.handleBadRequest(new IllegalArgumentException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid voting session request");
    }
}
