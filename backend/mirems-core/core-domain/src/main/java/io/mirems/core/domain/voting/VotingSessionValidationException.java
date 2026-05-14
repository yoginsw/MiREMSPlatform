package io.mirems.core.domain.voting;

import io.mirems.core.domain.exception.DomainException;

/** Raised when voting session invariants are violated. */
public class VotingSessionValidationException extends DomainException {
    public static final String ERROR_CODE = "MIR-VOTING-SESSION-VALIDATION-001";

    public VotingSessionValidationException(String message) {
        super(ERROR_CODE, message);
    }
}
