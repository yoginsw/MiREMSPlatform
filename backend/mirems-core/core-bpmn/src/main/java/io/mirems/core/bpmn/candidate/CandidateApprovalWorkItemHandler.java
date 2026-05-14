package io.mirems.core.bpmn.candidate;

import io.mirems.core.domain.contest.event.CandidateApprovedEvent;
import io.mirems.core.domain.contest.event.CandidateDisqualifiedEvent;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CandidateApprovalWorkItemHandler extends AbstractCandidateRegistrationWorkItemHandler {
    public CandidateRegistrationResult approve(CandidateRegistrationContext context) {
        context.request().candidate().approve();
        CandidateApprovedEvent event = context.request().candidate().pullDomainEvents().stream()
                .filter(CandidateApprovedEvent.class::isInstance)
                .map(CandidateApprovedEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CandidateApprovedEvent was not emitted"));
        return new CandidateRegistrationResult(CandidateRegistrationOutcome.APPROVED, event, false, List.of());
    }

    public CandidateRegistrationResult disqualify(CandidateRegistrationContext context, CandidateRegistrationOutcome outcome) {
        context.request().candidate().disqualify(context.reasons());
        CandidateDisqualifiedEvent event = context.request().candidate().pullDomainEvents().stream()
                .filter(CandidateDisqualifiedEvent.class::isInstance)
                .map(CandidateDisqualifiedEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CandidateDisqualifiedEvent was not emitted"));
        return new CandidateRegistrationResult(outcome, event, context.notificationSent(), context.reasons());
    }
}
