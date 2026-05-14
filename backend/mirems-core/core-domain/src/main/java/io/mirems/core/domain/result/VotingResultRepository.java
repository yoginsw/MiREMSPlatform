package io.mirems.core.domain.result;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only plus append-only persistence contract for immutable voting results. */
public interface VotingResultRepository {
    VotingResult save(VotingResult votingResult);

    Optional<VotingResult> findById(UUID id);

    List<VotingResult> findBySessionId(UUID sessionId);

    List<VotingResult> findByContestId(UUID contestId);
}
