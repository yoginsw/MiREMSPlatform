package io.mirems.core.bpmn.voter;

import java.util.Objects;

public record VoterEligibilityResult(boolean eligible, String reason) {
    public VoterEligibilityResult {
        Objects.requireNonNull(reason, "reason is required");
    }
}
