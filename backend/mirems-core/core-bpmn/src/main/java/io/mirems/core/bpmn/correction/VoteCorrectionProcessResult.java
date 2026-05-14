package io.mirems.core.bpmn.correction;

import io.mirems.core.domain.result.VoteCorrectedEvent;
import io.mirems.core.domain.result.VoteCorrection;
import java.util.Objects;

public record VoteCorrectionProcessResult(VoteCorrection correction, VoteCorrectedEvent event) {
    public VoteCorrectionProcessResult {
        Objects.requireNonNull(correction, "correction is required");
        Objects.requireNonNull(event, "event is required");
    }
}
