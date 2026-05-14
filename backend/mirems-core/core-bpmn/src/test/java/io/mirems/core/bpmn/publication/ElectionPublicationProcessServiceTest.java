package io.mirems.core.bpmn.publication;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionStatus;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.election.event.ElectionPublishedEvent;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ElectionPublicationProcessServiceTest {

    @Test
    void happyPathRequiresElectionAdminReviewThenPublishesElectionAndEmitsPublishedEvent() {
        Election election = election();
        ElectionPublicationProcessService service = new ElectionPublicationProcessService(
                new RequiredContestsValidationWorkItemHandler(),
                new BallotStyleCoverageValidationWorkItemHandler(),
                new PublishElectionWorkItemHandler());

        ElectionPublicationResult result = service.publishElection(new ElectionPublicationRequest(
                election,
                "ELECTION_ADMIN",
                true,
                true));

        assertThat(election.getElectionStatus()).isEqualTo(ElectionStatus.PUBLISHED);
        assertThat(result.passed()).isTrue();
        assertThat(result.event()).isInstanceOf(ElectionPublishedEvent.class);
        assertThat(result.failureReasons()).isEmpty();
    }

    @Test
    void validationFailurePathEmitsValidationFailedEventAndLeavesElectionInDraft() {
        Election election = election();
        ElectionPublicationProcessService service = new ElectionPublicationProcessService(
                new RequiredContestsValidationWorkItemHandler(),
                new BallotStyleCoverageValidationWorkItemHandler(),
                new PublishElectionWorkItemHandler());

        ElectionPublicationResult result = service.publishElection(new ElectionPublicationRequest(
                election,
                "ELECTION_ADMIN",
                false,
                true));

        assertThat(election.getElectionStatus()).isEqualTo(ElectionStatus.DRAFT);
        assertThat(result.passed()).isFalse();
        assertThat(result.event()).isInstanceOf(ElectionValidationFailedEvent.class);
        assertThat(result.failureReasons()).containsExactly("all required contests must be defined");
    }

    @Test
    void ballotStyleCoverageFailureEmitsValidationFailedEventAndLeavesElectionInDraft() {
        Election election = election();
        ElectionPublicationProcessService service = new ElectionPublicationProcessService(
                new RequiredContestsValidationWorkItemHandler(),
                new BallotStyleCoverageValidationWorkItemHandler(),
                new PublishElectionWorkItemHandler());

        ElectionPublicationResult result = service.publishElection(new ElectionPublicationRequest(
                election,
                "ELECTION_ADMIN",
                true,
                false));

        assertThat(election.getElectionStatus()).isEqualTo(ElectionStatus.DRAFT);
        assertThat(result.passed()).isFalse();
        assertThat(result.event()).isInstanceOf(ElectionValidationFailedEvent.class);
        assertThat(result.failureReasons()).containsExactly("ballot styles must cover all districts");
    }

    @Test
    void publishHandlerReturnsFailedResultWhenContextAlreadyHasValidationFailures() {
        Election election = election();
        ElectionPublicationContext context = ElectionPublicationContext.from(new ElectionPublicationRequest(
                election,
                "ELECTION_ADMIN",
                false,
                false));
        context.fail("pre-existing validation failure");

        ElectionPublicationResult result = new PublishElectionWorkItemHandler().publish(context);

        assertThat(election.getElectionStatus()).isEqualTo(ElectionStatus.DRAFT);
        assertThat(result.passed()).isFalse();
        assertThat(result.failureReasons()).containsExactly("pre-existing validation failure");
    }

    @Test
    void nonElectionAdminReviewFailsBeforeValidation() {
        Election election = election();
        ElectionPublicationProcessService service = new ElectionPublicationProcessService(
                new RequiredContestsValidationWorkItemHandler(),
                new BallotStyleCoverageValidationWorkItemHandler(),
                new PublishElectionWorkItemHandler());

        ElectionPublicationResult result = service.publishElection(new ElectionPublicationRequest(
                election,
                "VOTER",
                true,
                true));

        assertThat(election.getElectionStatus()).isEqualTo(ElectionStatus.DRAFT);
        assertThat(result.passed()).isFalse();
        assertThat(result.failureReasons()).containsExactly("ELECTION_ADMIN review is required");
    }

    private static Election election() {
        return Election.create(
                UUID.fromString("00000000-0000-0000-0000-000000000221"),
                "2026 General Election",
                ElectionType.PRESIDENTIAL,
                "National",
                LocalDate.of(2026, 11, 3),
                "US",
                "ext-us");
    }
}
