package io.mirems.core.infra.persistence.result;

import io.mirems.core.domain.result.VotingResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVotingResultJpaRepository extends JpaRepository<VotingResult, UUID> {
    List<VotingResult> findBySessionId(UUID sessionId);

    List<VotingResult> findByContestId(UUID contestId);
}
