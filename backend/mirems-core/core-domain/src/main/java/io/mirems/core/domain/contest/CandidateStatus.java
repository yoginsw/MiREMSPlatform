package io.mirems.core.domain.contest;

/** Lifecycle states for a candidate within a contest. */
public enum CandidateStatus {
    PENDING,
    APPROVED,
    WITHDRAWN,
    DISQUALIFIED;

    public boolean canTransitionTo(CandidateStatus target) {
        return switch (this) {
            case PENDING -> target == APPROVED || target == WITHDRAWN || target == DISQUALIFIED;
            case APPROVED, WITHDRAWN, DISQUALIFIED -> false;
        };
    }

    public void assertCanTransitionTo(CandidateStatus target) {
        if (!canTransitionTo(target)) {
            throw new CandidateStateException(this, target);
        }
    }
}
