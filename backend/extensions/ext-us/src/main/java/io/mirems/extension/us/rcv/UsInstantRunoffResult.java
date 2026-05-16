package io.mirems.extension.us.rcv;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record UsInstantRunoffResult(
        UUID contestId,
        Optional<UUID> winnerCandidateId,
        List<UsInstantRunoffRound> rounds,
        int exhaustedBallots,
        String auditSummary) {}
