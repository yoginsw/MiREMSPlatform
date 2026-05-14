package io.mirems.core.domain.ballot;

import io.mirems.core.domain.exception.DomainException;

/** Raised when a ballot domain invariant is violated. */
public class BallotValidationException extends DomainException {
    public static final String ERROR_CODE = "MIR-BALLOT-VALIDATION-001";

    public BallotValidationException(String message) {
        super(ERROR_CODE, message);
    }
}
