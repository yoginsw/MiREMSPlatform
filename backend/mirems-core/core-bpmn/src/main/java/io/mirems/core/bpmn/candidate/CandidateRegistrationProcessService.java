package io.mirems.core.bpmn.candidate;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CandidateRegistrationProcessService {
    private static final String REQUIRED_REVIEW_ROLE = "ELECTION_OFFICER";
    private static final Duration OFFICER_ACTION_TIMEOUT = Duration.ofHours(72);

    private final CandidateEligibilityCheckHandler eligibilityCheck;
    private final CandidateApprovalWorkItemHandler approvalHandler;
    private final CandidateRejectionNotificationWorkItemHandler notificationHandler;
    private final Clock clock;

    @Autowired
    public CandidateRegistrationProcessService(
            CandidateEligibilityCheckHandler eligibilityCheck,
            CandidateApprovalWorkItemHandler approvalHandler,
            CandidateRejectionNotificationWorkItemHandler notificationHandler) {
        this(eligibilityCheck, approvalHandler, notificationHandler, Clock.systemUTC());
    }

    public CandidateRegistrationProcessService(
            CandidateEligibilityCheckHandler eligibilityCheck,
            CandidateApprovalWorkItemHandler approvalHandler,
            CandidateRejectionNotificationWorkItemHandler notificationHandler,
            Clock clock) {
        this.eligibilityCheck = eligibilityCheck;
        this.approvalHandler = approvalHandler;
        this.notificationHandler = notificationHandler;
        this.clock = clock;
    }

    public CandidateRegistrationResult register(CandidateRegistrationRequest request) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.candidate(), "candidate is required");
        Objects.requireNonNull(request.officerDecision(), "officerDecision is required");
        Objects.requireNonNull(request.submittedAt(), "submittedAt is required");

        CandidateRegistrationContext context = CandidateRegistrationContext.from(request);
        eligibilityCheck.validate(context);
        if (!context.eligible()) {
            return rejectWithNotification(context, CandidateRegistrationOutcome.DISQUALIFIED);
        }

        if (isTimedOut(request.submittedAt())) {
            context.fail("ELECTION_OFFICER action not completed within 72h");
            return rejectWithNotification(context, CandidateRegistrationOutcome.TIMED_OUT);
        }

        if (!REQUIRED_REVIEW_ROLE.equals(request.reviewerRole())) {
            context.fail("ELECTION_OFFICER review is required");
            return rejectWithNotification(context, CandidateRegistrationOutcome.DISQUALIFIED);
        }

        return switch (request.officerDecision()) {
            case APPROVE -> approvalHandler.approve(context);
            case REJECT -> {
                context.fail("ELECTION_OFFICER rejected candidate documentation");
                yield rejectWithNotification(context, CandidateRegistrationOutcome.DISQUALIFIED);
            }
            case PENDING -> new CandidateRegistrationResult(CandidateRegistrationOutcome.PENDING_REVIEW, null, false, List.of());
        };
    }

    private CandidateRegistrationResult rejectWithNotification(
            CandidateRegistrationContext context,
            CandidateRegistrationOutcome outcome) {
        CandidateRegistrationContext notified = notificationHandler.send(context);
        return approvalHandler.disqualify(notified, outcome);
    }

    private boolean isTimedOut(OffsetDateTime submittedAt) {
        return Duration.between(submittedAt, OffsetDateTime.now(clock)).compareTo(OFFICER_ACTION_TIMEOUT) > 0;
    }
}
