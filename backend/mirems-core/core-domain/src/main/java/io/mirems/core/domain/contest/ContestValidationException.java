package io.mirems.core.domain.contest;

import io.mirems.core.domain.exception.DomainException;

/** Raised when a contest invariant is violated. */
public class ContestValidationException extends DomainException {
    public static final String ERROR_CODE = "MIR-CONTEST-VALIDATION-001";

    public ContestValidationException(String message) {
        super(ERROR_CODE, message);
    }
}
