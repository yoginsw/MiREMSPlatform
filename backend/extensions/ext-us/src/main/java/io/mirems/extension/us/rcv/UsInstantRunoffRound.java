package io.mirems.extension.us.rcv;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record UsInstantRunoffRound(
        int roundNumber,
        Map<UUID, Integer> activeTallies,
        int activeBallotCount,
        int majorityThreshold,
        Optional<UUID> eliminatedCandidateId,
        Optional<UUID> winnerCandidateId) {}
