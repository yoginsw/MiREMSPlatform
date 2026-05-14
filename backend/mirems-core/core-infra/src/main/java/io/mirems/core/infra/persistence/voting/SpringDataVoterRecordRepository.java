package io.mirems.core.infra.persistence.voting;

import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVoterRecordRepository extends JpaRepository<VoterRecord, UUID> {
    List<VoterRecord> findByRegistrationStatus(RegistrationStatus registrationStatus);
}
