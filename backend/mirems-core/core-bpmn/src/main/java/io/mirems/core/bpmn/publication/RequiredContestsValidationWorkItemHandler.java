package io.mirems.core.bpmn.publication;

import org.springframework.stereotype.Component;

@Component
public class RequiredContestsValidationWorkItemHandler extends AbstractElectionPublicationWorkItemHandler {
    static final String FAILURE_REASON = "all required contests must be defined";

    public void validate(ElectionPublicationContext context) {
        if (!context.request().requiredContestsDefined()) {
            context.fail(FAILURE_REASON);
        }
    }
}
