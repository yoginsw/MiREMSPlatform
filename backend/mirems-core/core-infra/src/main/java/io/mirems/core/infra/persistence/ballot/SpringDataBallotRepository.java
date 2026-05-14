package io.mirems.core.infra.persistence.ballot;

import io.mirems.core.domain.ballot.Ballot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataBallotRepository extends JpaRepository<Ballot, UUID> {
    List<Ballot> findByElectionId(UUID electionId);

    Optional<Ballot> findByElectionIdAndActiveTrue(UUID electionId);
}
