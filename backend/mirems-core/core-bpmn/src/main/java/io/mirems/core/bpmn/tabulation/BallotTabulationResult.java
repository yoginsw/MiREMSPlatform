package io.mirems.core.bpmn.tabulation;

import io.mirems.core.domain.result.TabulationCompletedEvent;
import io.mirems.core.domain.result.TabulationReport;
import java.util.Objects;

public record BallotTabulationResult(
        boolean completed,
        boolean published,
        TabulationReport report,
        TabulationCompletedEvent completedEvent) {
    public BallotTabulationResult {
        Objects.requireNonNull(report, "report is required");
        Objects.requireNonNull(completedEvent, "completedEvent is required");
    }
}
