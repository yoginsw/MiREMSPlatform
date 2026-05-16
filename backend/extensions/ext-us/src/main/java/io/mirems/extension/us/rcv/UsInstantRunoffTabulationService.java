package io.mirems.extension.us.rcv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UsInstantRunoffTabulationService {
    public UsInstantRunoffResult tabulate(UsInstantRunoffRequest request) {
        Objects.requireNonNull(request, "request is required");
        UUID contestId = Objects.requireNonNull(request.contestId(), "contestId is required");
        Set<UUID> candidateIds = Set.copyOf(Objects.requireNonNull(request.candidateIds(), "candidateIds is required"));
        if (candidateIds.isEmpty()) {
            throw new IllegalArgumentException("candidateIds must not be empty");
        }
        List<UsRankedChoiceBallot> ballots = List.copyOf(Objects.requireNonNull(request.ballots(), "ballots is required"));
        if (ballots.isEmpty()) {
            throw new IllegalArgumentException("ballots must not be empty");
        }
        ballots.forEach(ballot -> validateBallot(contestId, candidateIds, ballot));

        Set<UUID> activeCandidates = new HashSet<>(candidateIds);
        List<UsInstantRunoffRound> rounds = new ArrayList<>();
        int roundNumber = 1;
        Optional<UUID> winner = Optional.empty();

        while (!activeCandidates.isEmpty() && winner.isEmpty()) {
            Map<UUID, Integer> tallies = countActiveChoices(ballots, activeCandidates);
            int activeBallotCount = tallies.values().stream().mapToInt(Integer::intValue).sum();
            int majorityThreshold = Math.floorDiv(activeBallotCount, 2) + 1;
            Optional<UUID> roundWinner = findWinner(tallies, majorityThreshold);
            Optional<UUID> eliminated = Optional.empty();
            if (roundWinner.isPresent()) {
                winner = roundWinner;
            } else if (activeCandidates.size() == 1) {
                winner = activeCandidates.stream().findFirst();
            } else {
                eliminated = Optional.of(selectEliminationCandidate(tallies, activeCandidates));
                activeCandidates.remove(eliminated.get());
            }
            rounds.add(new UsInstantRunoffRound(
                    roundNumber,
                    Map.copyOf(tallies),
                    activeBallotCount,
                    majorityThreshold,
                    eliminated,
                    roundWinner));
            roundNumber++;
        }

        int exhaustedBallots = countExhaustedBallots(ballots, activeCandidates);
        return new UsInstantRunoffResult(
                contestId,
                winner,
                List.copyOf(rounds),
                exhaustedBallots,
                auditSummary(contestId, winner, rounds, exhaustedBallots));
    }

    private static void validateBallot(UUID contestId, Set<UUID> candidateIds, UsRankedChoiceBallot ballot) {
        Objects.requireNonNull(ballot, "ballot is required");
        if (!contestId.equals(ballot.contestId())) {
            throw new IllegalArgumentException("ballot contestId must match request contestId");
        }
        if (!candidateIds.containsAll(ballot.rankedCandidateIds())) {
            throw new IllegalArgumentException("ballot rankedCandidateIds must be inside candidateIds");
        }
    }

    private static Map<UUID, Integer> countActiveChoices(List<UsRankedChoiceBallot> ballots, Set<UUID> activeCandidates) {
        Map<UUID, Integer> tallies = new HashMap<>();
        activeCandidates.forEach(candidateId -> tallies.put(candidateId, 0));
        for (UsRankedChoiceBallot ballot : ballots) {
            ballot.firstActiveChoice(activeCandidates).ifPresent(candidateId -> tallies.merge(candidateId, 1, Integer::sum));
        }
        return tallies;
    }

    private static Optional<UUID> findWinner(Map<UUID, Integer> tallies, int majorityThreshold) {
        return tallies.entrySet().stream()
                .filter(entry -> entry.getValue() >= majorityThreshold)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    private static UUID selectEliminationCandidate(Map<UUID, Integer> tallies, Set<UUID> activeCandidates) {
        return activeCandidates.stream()
                .min(Comparator.<UUID>comparingInt(candidateId -> tallies.getOrDefault(candidateId, 0))
                        .thenComparing(Comparator.reverseOrder()))
                .orElseThrow();
    }

    private static int countExhaustedBallots(List<UsRankedChoiceBallot> ballots, Set<UUID> activeCandidates) {
        return (int) ballots.stream().filter(ballot -> ballot.firstActiveChoice(activeCandidates).isEmpty()).count();
    }

    private static String auditSummary(
            UUID contestId,
            Optional<UUID> winner,
            List<UsInstantRunoffRound> rounds,
            int exhaustedBallots) {
        return "contest=" + contestId
                + "; winner=" + winner.map(UUID::toString).orElse("none")
                + "; rounds=" + rounds.size()
                + "; exhaustedBallots=" + exhaustedBallots;
    }
}
