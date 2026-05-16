package io.mirems.core.domain.voting;

/** Extension hook for country-specific voting-session opening rules. */
@FunctionalInterface
public interface VotingSessionOpeningPolicy {
    void validate(VotingSessionOpeningContext context);
}
