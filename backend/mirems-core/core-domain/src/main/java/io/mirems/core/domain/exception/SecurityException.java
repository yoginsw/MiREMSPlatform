package io.mirems.core.domain.exception;

/** Raised when authentication, role authorization, or election-scope authorization fails. */
public class SecurityException extends MiremsException {
    public SecurityException(String errorCode, String message) {
        super(errorCode, "Security policy violation", message);
    }

    public SecurityException(String errorCode, String message, Throwable cause) {
        super(errorCode, "Security policy violation", message, cause);
    }
}
