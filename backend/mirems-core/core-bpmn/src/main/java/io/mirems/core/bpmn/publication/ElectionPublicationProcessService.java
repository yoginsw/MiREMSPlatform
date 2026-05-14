package io.mirems.core.bpmn.publication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ElectionPublicationProcessService {
    private static final String REQUIRED_REVIEW_ROLE = "ELECTION_ADMIN";

    private final RequiredContestsValidationWorkItemHandler contestsValidation;
    private final BallotStyleCoverageValidationWorkItemHandler ballotStyleValidation;
    private final PublishElectionWorkItemHandler publication;

    public ElectionPublicationProcessService(
            RequiredContestsValidationWorkItemHandler contestsValidation,
            BallotStyleCoverageValidationWorkItemHandler ballotStyleValidation,
            PublishElectionWorkItemHandler publication) {
        this.contestsValidation = contestsValidation;
        this.ballotStyleValidation = ballotStyleValidation;
        this.publication = publication;
    }

    public ElectionPublicationResult publishElection(ElectionPublicationRequest request) {
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.election(), "election is required");

        ElectionPublicationContext context = ElectionPublicationContext.from(request);
        if (!REQUIRED_REVIEW_ROLE.equals(request.reviewerRole())) {
            context.fail("ELECTION_ADMIN review is required");
            return failedResult(context);
        }

        contestsValidation.validate(context);
        ballotStyleValidation.validate(context);

        if (!context.passed()) {
            return failedResult(context);
        }
        return publication.publish(context);
    }

    private ElectionPublicationResult failedResult(ElectionPublicationContext context) {
        return new ElectionPublicationResult(
                false,
                new ElectionValidationFailedEvent(
                        context.request().election().getId(),
                        List.copyOf(context.failureReasons()),
                        OffsetDateTime.now()),
                context.failureReasons());
    }
}
