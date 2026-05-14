package io.mirems.core.domain.voting;

import io.mirems.core.domain.exception.DomainException;

/** Raised when a voting session lifecycle transition is invalid. */
public class VotingSessionStateException extends DomainException {
    public static final String ERROR_CODE = "MIR-VOTING-SESSION-STATE-001";

    public VotingSessionStateException(SessionStatus currentStatus, SessionStatus targetStatus) {
        super(
                ERROR_CODE,
                "Invalid voting session state transition from %s to %s".formatted(currentStatus, targetStatus));
    }
}
