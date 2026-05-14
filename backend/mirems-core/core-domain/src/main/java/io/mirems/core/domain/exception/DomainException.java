package io.mirems.core.domain.exception;

/** Raised when a domain invariant or election business rule is violated. */
public class DomainException extends MiremsException {
    public DomainException(String errorCode, String message) {
        super(errorCode, "Domain rule violation", message);
    }

    public DomainException(String errorCode, String message, Throwable cause) {
        super(errorCode, "Domain rule violation", message, cause);
    }
}
