package io.mirems.core.infra.persistence.result;

import io.mirems.core.bpmn.tabulation.VotingResultLoader;
import io.mirems.core.domain.result.VotingResult;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(SpringDataVotingResultJpaRepository.class)
public class JpaVotingResultLoader implements VotingResultLoader {
    private final SpringDataVotingResultJpaRepository votingResultRepository;

    public JpaVotingResultLoader(SpringDataVotingResultJpaRepository votingResultRepository) {
        this.votingResultRepository = Objects.requireNonNull(votingResultRepository, "votingResultRepository is required");
    }

    @Override
    public List<VotingResult> loadCommittedResults(UUID electionId) {
        return votingResultRepository.findBySessionElectionId(Objects.requireNonNull(electionId, "electionId is required"));
    }
}
