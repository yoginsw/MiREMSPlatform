package io.mirems.core.infra.persistence.result;

import io.mirems.core.domain.result.TabulationReport;
import io.mirems.core.domain.result.TabulationReportRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTabulationReportRepository
        extends JpaRepository<TabulationReport, UUID>, TabulationReportRepository {
    @Override
    Optional<TabulationReport> findByElectionId(UUID electionId);
}
