package io.mirems.core.bpmn.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.domain.contest.Candidate;
import io.mirems.core.domain.contest.CandidateStatus;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.contest.event.CandidateApprovedEvent;
import io.mirems.core.domain.contest.event.CandidateDisqualifiedEvent;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CandidateRegistrationProcessServiceTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void happyPathApprovesEligibleCandidateAfterElectionOfficerReviewAndEmitsApprovedEvent() {
        Candidate candidate = candidate();
        CandidateRegistrationProcessService service = service();

        CandidateRegistrationResult result = service.register(new CandidateRegistrationRequest(
                candidate,
                45,
                true,
                "ELECTION_OFFICER",
                CandidateOfficerDecision.APPROVE,
                submittedAt(2)));

        assertThat(candidate.getCandidateStatus()).isEqualTo(CandidateStatus.APPROVED);
        assertThat(result.status()).isEqualTo(CandidateRegistrationOutcome.APPROVED);
        assertThat(result.event()).isInstanceOf(CandidateApprovedEvent.class);
        assertThat(result.notificationSent()).isFalse();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void officerRejectionDisqualifiesCandidateEmitsEventAndSendsNotification() {
        Candidate candidate = candidate();
        CandidateRegistrationProcessService service = service();

        CandidateRegistrationResult result = service.register(new CandidateRegistrationRequest(
                candidate,
                45,
                true,
                "ELECTION_OFFICER",
                CandidateOfficerDecision.REJECT,
                submittedAt(2)));

        assertThat(candidate.getCandidateStatus()).isEqualTo(CandidateStatus.DISQUALIFIED);
        assertThat(result.status()).isEqualTo(CandidateRegistrationOutcome.DISQUALIFIED);
        assertThat(result.event()).isInstanceOf(CandidateDisqualifiedEvent.class);
        assertThat(result.notificationSent()).isTrue();
        assertThat(result.reasons()).containsExactly("ELECTION_OFFICER rejected candidate documentation");
    }

    @Test
    void timeoutAutoRejectsWhenOfficerTakesNoActionWithinSeventyTwoHours() {
        Candidate candidate = candidate();
        CandidateRegistrationProcessService service = service();

        CandidateRegistrationResult result = service.register(new CandidateRegistrationRequest(
                candidate,
                45,
                true,
                "ELECTION_OFFICER",
                CandidateOfficerDecision.PENDING,
                submittedAt(73)));

        assertThat(candidate.getCandidateStatus()).isEqualTo(CandidateStatus.DISQUALIFIED);
        assertThat(result.status()).isEqualTo(CandidateRegistrationOutcome.TIMED_OUT);
        assertThat(result.event()).isInstanceOf(CandidateDisqualifiedEvent.class);
        assertThat(result.notificationSent()).isTrue();
        assertThat(result.reasons()).containsExactly("ELECTION_OFFICER action not completed within 72h");
    }

    @Test
    void ineligibleCandidateIsRejectedBeforeOfficerReview() {
        Candidate candidate = candidate();
        CandidateRegistrationProcessService service = service();

        CandidateRegistrationResult result = service.register(new CandidateRegistrationRequest(
                candidate,
                20,
                false,
                "ELECTION_OFFICER",
                CandidateOfficerDecision.APPROVE,
                submittedAt(2)));

        assertThat(candidate.getCandidateStatus()).isEqualTo(CandidateStatus.DISQUALIFIED);
        assertThat(result.status()).isEqualTo(CandidateRegistrationOutcome.DISQUALIFIED);
        assertThat(result.event()).isInstanceOf(CandidateDisqualifiedEvent.class);
        assertThat(result.notificationSent()).isTrue();
        assertThat(result.reasons()).containsExactly(
                "candidate age must be at least 35",
                "candidate residency must be verified");
    }

    private static CandidateRegistrationProcessService service() {
        return new CandidateRegistrationProcessService(
                new CandidateEligibilityCheckHandler(),
                new CandidateApprovalWorkItemHandler(),
                new CandidateRejectionNotificationWorkItemHandler(),
                FIXED_CLOCK);
    }

    private static OffsetDateTime submittedAt(int hoursAgo) {
        return OffsetDateTime.now(FIXED_CLOCK).minusHours(hoursAgo);
    }

    private static Candidate candidate() {
        Election election = Election.create(
                UUID.fromString("00000000-0000-0000-0000-000000000222"),
                "2026 Presidential Election",
                ElectionType.PRESIDENTIAL,
                "National",
                LocalDate.of(2026, 11, 3),
                "US",
                "ext-us");
        Contest contest = Contest.create(
                UUID.fromString("00000000-0000-0000-0000-000000000223"),
                election,
                ContestType.CANDIDATE_CHOICE,
                "President",
                1,
                1);
        return contest.addCandidate(
                UUID.fromString("00000000-0000-0000-0000-000000000224"),
                "Alex Candidate",
                "Independent");
    }
}
