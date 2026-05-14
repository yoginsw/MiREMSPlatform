package io.mirems.core.bpmn.candidate;

import java.util.ArrayList;
import java.util.List;

public record CandidateRegistrationContext(
        CandidateRegistrationRequest request,
        List<String> reasons,
        boolean notificationSent) {
    public CandidateRegistrationContext {
        reasons = new ArrayList<>(reasons == null ? List.of() : reasons);
    }

    public static CandidateRegistrationContext from(CandidateRegistrationRequest request) {
        return new CandidateRegistrationContext(request, new ArrayList<>(), false);
    }

    public CandidateRegistrationContext withNotificationSent() {
        return new CandidateRegistrationContext(request, reasons, true);
    }

    public void fail(String reason) {
        reasons.add(reason);
    }

    public boolean eligible() {
        return reasons.isEmpty();
    }
}
