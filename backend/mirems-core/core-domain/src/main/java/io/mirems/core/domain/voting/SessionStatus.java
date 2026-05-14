package io.mirems.core.domain.voting;

/** Lifecycle states for an individual voting session. */
public enum SessionStatus {
    OPENED,
    CAST,
    SPOILED,
    EXPIRED;

    public boolean canTransitionTo(SessionStatus target) {
        return switch (this) {
            case OPENED -> target == CAST || target == SPOILED || target == EXPIRED;
            case CAST, SPOILED, EXPIRED -> false;
        };
    }

    public void assertCanTransitionTo(SessionStatus target) {
        if (!canTransitionTo(target)) {
            throw new VotingSessionStateException(this, target);
        }
    }
}
