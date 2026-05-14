package io.mirems.core.api.election;

import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ElectionController.class)
public class ElectionExceptionHandler {
    @ExceptionHandler(ElectionController.ElectionNotFoundException.class)
    ResponseEntity<ProblemDetail> handleElectionNotFound(ElectionController.ElectionNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Election not found");
        problem.setType(URI.create("https://mirems.io/problems/election-not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(ElectionController.ElectionServiceUnavailableException.class)
    ResponseEntity<ProblemDetail> handleElectionServiceUnavailable(
            ElectionController.ElectionServiceUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setTitle("Election service unavailable");
        problem.setType(URI.create("https://mirems.io/problems/election-service-unavailable"));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Entity not found");
        problem.setType(URI.create("https://mirems.io/problems/entity-not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
