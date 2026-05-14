package io.mirems.core.domain.result;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Append-only repository port for vote corrections. */
public interface VoteCorrectionRepository {
    VoteCorrection save(VoteCorrection correction);

    Optional<VoteCorrection> findById(UUID id);

    List<VoteCorrection> findByOriginalVotingResultId(UUID originalVotingResultId);
}
