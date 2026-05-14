package io.mirems.core.domain.result;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** Domain event emitted when a tabulation report is locked with a tamper-evident hash. */
public record TabulationCompletedEvent(UUID reportId, UUID electionId, String reportHash, OffsetDateTime completedAt) {
    public TabulationCompletedEvent {
        Objects.requireNonNull(reportId, "reportId is required");
        Objects.requireNonNull(electionId, "electionId is required");
        if (reportHash == null || reportHash.isBlank()) {
            throw new IllegalArgumentException("reportHash is required");
        }
        Objects.requireNonNull(completedAt, "completedAt is required");
    }
}
