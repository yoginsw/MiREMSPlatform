package io.mirems.core.infra.persistence.result;

import io.mirems.core.domain.result.CorrectionStatus;
import io.mirems.core.domain.result.VoteCorrection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVoteCorrectionRepository extends JpaRepository<VoteCorrection, UUID> {
    List<VoteCorrection> findByOriginalVotingResultId(UUID originalVotingResultId);

    List<VoteCorrection> findByCorrectionStatus(CorrectionStatus correctionStatus);
}
