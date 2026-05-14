package io.mirems.core.infra.persistence.contest;

import io.mirems.core.domain.contest.Candidate;
import io.mirems.core.domain.contest.CandidateStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCandidateRepository extends JpaRepository<Candidate, UUID> {
    List<Candidate> findByContestId(UUID contestId);

    List<Candidate> findByCandidateStatus(CandidateStatus candidateStatus);
}
