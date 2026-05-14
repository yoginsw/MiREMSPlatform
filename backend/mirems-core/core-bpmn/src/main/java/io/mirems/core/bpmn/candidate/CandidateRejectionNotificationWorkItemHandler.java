package io.mirems.core.bpmn.candidate;

import org.springframework.stereotype.Component;

@Component
public class CandidateRejectionNotificationWorkItemHandler extends AbstractCandidateRegistrationWorkItemHandler {
    public CandidateRegistrationContext send(CandidateRegistrationContext context) {
        return context.withNotificationSent();
    }
}
