package io.mirems.core.domain.exception;

/** Raised when inbound command, DTO, or policy input validation fails. */
public class ValidationException extends MiremsException {
    public ValidationException(String errorCode, String message) {
        super(errorCode, "Validation failed", message);
    }

    public ValidationException(String errorCode, String message, Throwable cause) {
        super(errorCode, "Validation failed", message, cause);
    }
}
