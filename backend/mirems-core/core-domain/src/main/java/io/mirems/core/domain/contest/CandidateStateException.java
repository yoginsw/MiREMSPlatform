package io.mirems.core.domain.contest;

import io.mirems.core.domain.exception.DomainException;

/** Raised when a candidate lifecycle transition is invalid. */
public class CandidateStateException extends DomainException {
    public static final String ERROR_CODE = "MIR-CANDIDATE-STATE-001";

    public CandidateStateException(CandidateStatus currentStatus, CandidateStatus targetStatus) {
        super(
                ERROR_CODE,
                "Invalid candidate state transition from %s to %s".formatted(currentStatus, targetStatus));
    }
}
