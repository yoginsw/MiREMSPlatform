package io.mirems.core.domain.election;

/** Lifecycle states for an election aggregate. */
public enum ElectionStatus {
    DRAFT,
    PUBLISHED,
    ACTIVE,
    CLOSED,
    CERTIFIED;

    public boolean canTransitionTo(ElectionStatus target) {
        return switch (this) {
            case DRAFT -> target == PUBLISHED;
            case PUBLISHED -> target == ACTIVE;
            case ACTIVE -> target == CLOSED;
            case CLOSED -> target == CERTIFIED;
            case CERTIFIED -> false;
        };
    }

    public void assertCanTransitionTo(ElectionStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidElectionStateException(this, target);
        }
    }
}
