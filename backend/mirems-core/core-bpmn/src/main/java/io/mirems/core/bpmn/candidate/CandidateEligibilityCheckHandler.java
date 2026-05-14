package io.mirems.core.bpmn.candidate;

import org.springframework.stereotype.Component;

@Component
public class CandidateEligibilityCheckHandler extends AbstractCandidateRegistrationWorkItemHandler {
    static final int MINIMUM_AGE = 35;

    public void validate(CandidateRegistrationContext context) {
        if (context.request().candidateAge() < MINIMUM_AGE) {
            context.fail("candidate age must be at least 35");
        }
        if (!context.request().residencyVerified()) {
            context.fail("candidate residency must be verified");
        }
    }
}
