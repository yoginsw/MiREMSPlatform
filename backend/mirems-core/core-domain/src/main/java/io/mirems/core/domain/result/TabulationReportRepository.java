package io.mirems.core.domain.result;

import java.util.Optional;
import java.util.UUID;

/** Read-only plus append-only persistence contract for immutable tabulation reports. */
public interface TabulationReportRepository {
    TabulationReport save(TabulationReport report);

    Optional<TabulationReport> findById(UUID id);

    Optional<TabulationReport> findByElectionId(UUID electionId);
}
