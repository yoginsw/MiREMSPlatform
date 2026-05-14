package io.mirems.core.domain.voting.encryption;

/** Runtime exception raised when PII cryptographic processing fails. */
public class PiiEncryptionException extends RuntimeException {
    public PiiEncryptionException(String message) {
        super(message);
    }

    public PiiEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
