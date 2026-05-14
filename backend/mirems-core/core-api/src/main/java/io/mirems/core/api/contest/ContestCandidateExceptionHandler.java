package io.mirems.core.api.contest;

import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {ContestController.class, CandidateController.class})
public class ContestCandidateExceptionHandler {
    @ExceptionHandler(ContestController.ContestNotFoundException.class)
    ResponseEntity<ProblemDetail> handleContestNotFound(ContestController.ContestNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Contest not found");
        problem.setType(URI.create("https://mirems.io/problems/contest-not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(CandidateController.CandidateNotFoundException.class)
    ResponseEntity<ProblemDetail> handleCandidateNotFound(CandidateController.CandidateNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Candidate not found");
        problem.setType(URI.create("https://mirems.io/problems/candidate-not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler({
        ContestController.ContestServiceUnavailableException.class,
        CandidateController.CandidateServiceUnavailableException.class,
        CandidateController.CandidateRegistrationUnavailableException.class
    })
    ResponseEntity<ProblemDetail> handleUnavailable(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setTitle("Contest or candidate service unavailable");
        problem.setType(URI.create("https://mirems.io/problems/contest-candidate-service-unavailable"));
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
