package io.mirems.core.bpmn.candidate;

import java.util.List;

public record CandidateRegistrationResult(
        CandidateRegistrationOutcome status,
        Object event,
        boolean notificationSent,
        List<String> reasons) {
    public CandidateRegistrationResult {
        reasons = List.copyOf(reasons == null ? List.of() : reasons);
    }
}
