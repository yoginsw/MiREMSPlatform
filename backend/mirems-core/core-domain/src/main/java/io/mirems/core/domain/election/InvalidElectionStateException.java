package io.mirems.core.domain.election;

import io.mirems.core.domain.exception.DomainException;

/** Raised when an election lifecycle transition violates the core state machine. */
public class InvalidElectionStateException extends DomainException {
    public static final String ERROR_CODE = "MIR-ELECTION-STATE-001";

    public InvalidElectionStateException(ElectionStatus currentStatus, ElectionStatus targetStatus) {
        super(
                ERROR_CODE,
                "Invalid election state transition from %s to %s".formatted(currentStatus, targetStatus));
    }
}
