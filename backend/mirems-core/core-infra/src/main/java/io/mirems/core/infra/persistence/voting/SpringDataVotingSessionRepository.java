package io.mirems.core.infra.persistence.voting;

import io.mirems.core.domain.voting.SessionStatus;
import io.mirems.core.domain.voting.VotingSession;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVotingSessionRepository extends JpaRepository<VotingSession, UUID> {
    List<VotingSession> findByVoterRecordId(UUID voterRecordId);

    List<VotingSession> findByElectionId(UUID electionId);

    List<VotingSession> findBySessionStatus(SessionStatus sessionStatus);
}
