package io.mirems.core.bpmn.publication;

import org.springframework.stereotype.Component;

@Component
public class BallotStyleCoverageValidationWorkItemHandler extends AbstractElectionPublicationWorkItemHandler {
    static final String FAILURE_REASON = "ballot styles must cover all districts";

    public void validate(ElectionPublicationContext context) {
        if (!context.request().ballotStylesCoverAllDistricts()) {
            context.fail(FAILURE_REASON);
        }
    }
}
