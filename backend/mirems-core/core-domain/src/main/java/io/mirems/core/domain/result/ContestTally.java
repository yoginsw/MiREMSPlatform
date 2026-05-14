package io.mirems.core.domain.result;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable tally for one contest in a tabulation report. */
public record ContestTally(UUID contestId, Map<UUID, Integer> candidateTallies, int ballotsCounted) {
    public ContestTally {
        Objects.requireNonNull(contestId, "contestId is required");
        candidateTallies = Map.copyOf(Objects.requireNonNull(candidateTallies, "candidateTallies is required"));
        if (candidateTallies.isEmpty()) {
            throw new IllegalArgumentException("candidateTallies must not be empty");
        }
        if (candidateTallies.values().stream().anyMatch(count -> count == null || count < 0)) {
            throw new IllegalArgumentException("candidateTallies must contain non-negative counts");
        }
        if (ballotsCounted < 0) {
            throw new IllegalArgumentException("ballotsCounted must be non-negative");
        }
    }
}
