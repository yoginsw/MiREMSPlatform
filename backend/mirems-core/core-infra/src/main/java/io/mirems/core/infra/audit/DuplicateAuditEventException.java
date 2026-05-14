package io.mirems.core.infra.audit;

/** Raised when append-only audit storage sees an already persisted event ID. */
public class DuplicateAuditEventException extends RuntimeException {
    public DuplicateAuditEventException(String message) {
        super(message);
    }
}
