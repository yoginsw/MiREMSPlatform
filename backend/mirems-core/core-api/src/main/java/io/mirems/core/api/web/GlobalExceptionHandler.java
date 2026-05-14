package io.mirems.core.api.web;

import io.mirems.core.domain.exception.DomainException;
import io.mirems.core.domain.exception.MiremsException;
import io.mirems.core.domain.exception.ProcessException;
import io.mirems.core.domain.exception.SecurityException;
import io.mirems.core.domain.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ValidationException.class)
    ProblemDetail handleValidationException(ValidationException exception, HttpServletRequest request) {
        return problemDetail(HttpStatus.BAD_REQUEST, exception, request);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomainException(DomainException exception, HttpServletRequest request) {
        return problemDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception, request);
    }

    @ExceptionHandler(ProcessException.class)
    ProblemDetail handleProcessException(ProcessException exception, HttpServletRequest request) {
        return problemDetail(HttpStatus.CONFLICT, exception, request);
    }

    @ExceptionHandler(SecurityException.class)
    ProblemDetail handleSecurityException(SecurityException exception, HttpServletRequest request) {
        return problemDetail(HttpStatus.FORBIDDEN, exception, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        detail.setTitle("Validation failed");
        detail.setType(URI.create("urn:mirems:error:validation"));
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("errorCode", "MIR-VAL-000");
        detail.setProperty("violations", exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::fieldViolation)
                .toList());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred");
        detail.setTitle("Internal server error");
        detail.setType(URI.create("urn:mirems:error:internal"));
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("errorCode", "MIR-INT-000");
        detail.setProperty("exceptionType", exception.getClass().getSimpleName());
        return detail;
    }

    private ProblemDetail problemDetail(HttpStatus status, MiremsException exception, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        detail.setTitle(exception.getTitle());
        detail.setType(URI.create("urn:mirems:error:" + exception.getErrorCode()));
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("errorCode", exception.getErrorCode());
        detail.setProperty("exceptionType", exception.getClass().getSimpleName());
        return detail;
    }

    private static FieldViolation fieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }

    private record FieldViolation(String field, String message) {
    }
}
