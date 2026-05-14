package io.mirems.core.infra.persistence.contest;

import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataContestRepository extends JpaRepository<Contest, UUID> {
    List<Contest> findByElectionId(UUID electionId);

    List<Contest> findByContestType(ContestType contestType);
}
