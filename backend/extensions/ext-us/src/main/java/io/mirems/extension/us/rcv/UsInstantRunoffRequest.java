package io.mirems.extension.us.rcv;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UsInstantRunoffRequest(UUID contestId, Set<UUID> candidateIds, List<UsRankedChoiceBallot> ballots) {}
