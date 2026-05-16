package io.mirems.extension.us.rcv;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record UsRankedChoiceBallot(UUID contestId, List<UUID> rankedCandidateIds) {
    public UsRankedChoiceBallot {
        contestId = Objects.requireNonNull(contestId, "contestId is required");
        rankedCandidateIds = List.copyOf(Objects.requireNonNull(rankedCandidateIds, "rankedCandidateIds is required"));
        if (rankedCandidateIds.isEmpty()) {
            throw new IllegalArgumentException("rankedCandidateIds must contain at least one candidate");
        }
        if (new HashSet<>(rankedCandidateIds).size() != rankedCandidateIds.size()) {
            throw new IllegalArgumentException("rankedCandidateIds must not contain duplicates");
        }
    }

    public static UsRankedChoiceBallot cast(UUID contestId, List<UUID> rankedCandidateIds, Set<UUID> contestCandidateIds) {
        List<UUID> ranks = List.copyOf(Objects.requireNonNull(rankedCandidateIds, "rankedCandidateIds is required"));
        Set<UUID> candidateIds = Set.copyOf(Objects.requireNonNull(contestCandidateIds, "contestCandidateIds is required"));
        if (ranks.size() > candidateIds.size()) {
            throw new IllegalArgumentException("rankedCandidateIds cannot exceed candidate count");
        }
        if (new HashSet<>(ranks).size() != ranks.size()) {
            throw new IllegalArgumentException("rankedCandidateIds must not contain duplicates");
        }
        if (!candidateIds.containsAll(ranks)) {
            throw new IllegalArgumentException("rankedCandidateIds contains a candidate outside the contest");
        }
        return new UsRankedChoiceBallot(contestId, ranks);
    }

    public Optional<UUID> firstActiveChoice(Set<UUID> activeCandidateIds) {
        Set<UUID> active = Set.copyOf(Objects.requireNonNull(activeCandidateIds, "activeCandidateIds is required"));
        return rankedCandidateIds.stream().filter(active::contains).findFirst();
    }
}
