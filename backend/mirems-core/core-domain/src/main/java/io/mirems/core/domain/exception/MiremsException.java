package io.mirems.core.domain.exception;

/** Base unchecked exception for MiREMS domain, process, validation, and security failures. */
public abstract class MiremsException extends RuntimeException {
    private final String errorCode;
    private final String title;

    protected MiremsException(String errorCode, String title, String message) {
        super(message);
        this.errorCode = errorCode;
        this.title = title;
    }

    protected MiremsException(String errorCode, String title, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.title = title;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTitle() {
        return title;
    }
}
