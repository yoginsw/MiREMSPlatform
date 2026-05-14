package io.mirems.core.domain.contest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.contest.event.CandidateApprovedEvent;
import io.mirems.core.domain.contest.event.CandidateWithdrawnEvent;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContestTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4b7e-77d3-7c22-b7ec-6e89229b7d43");
    private static final UUID CONTEST_ID = UUID.fromString("018f4b7e-88e4-7d33-b8fd-7f90330c8e54");
    private static final UUID CANDIDATE_ID = UUID.fromString("018f4b7e-99f5-7e44-c90e-8091441d9f65");

    @Test
    void createCandidateChoiceContestInitializesFields() {
        Election election = election();

        Contest contest = Contest.create(
                CONTEST_ID,
                election,
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1);

        assertEquals(CONTEST_ID, contest.getId());
        assertSame(election, contest.getElection());
        assertEquals(ContestType.CANDIDATE_CHOICE, contest.getContestType());
        assertEquals("Mayor", contest.getName());
        assertEquals(1, contest.getSeats());
        assertEquals(1, contest.getVoteLimit());
        assertTrue(contest.getCandidates().isEmpty());
    }

    @Test
    void contestRejectsVoteLimitGreaterThanSeats() {
        ContestValidationException exception = assertThrows(ContestValidationException.class, () -> Contest.create(
                CONTEST_ID,
                election(),
                ContestType.CANDIDATE_CHOICE,
                "City Council",
                2,
                3));

        assertEquals("MIR-CONTEST-VALIDATION-001", exception.getErrorCode());
        assertEquals("Domain rule violation", exception.getTitle());
        assertTrue(exception.getMessage().contains("voteLimit"));
        assertTrue(exception.getMessage().contains("seats"));
    }

    @Test
    void contestRejectsInvalidRequiredFields() {
        assertThrows(NullPointerException.class, () -> Contest.create(
                null,
                election(),
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1));
        assertThrows(NullPointerException.class, () -> Contest.create(
                CONTEST_ID,
                null,
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1));
        assertThrows(IllegalArgumentException.class, () -> Contest.create(
                CONTEST_ID,
                election(),
                ContestType.CANDIDATE_CHOICE,
                " ",
                1,
                1));
        assertThrows(ContestValidationException.class, () -> Contest.create(
                CONTEST_ID,
                election(),
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                0,
                1));
        assertThrows(ContestValidationException.class, () -> Contest.create(
                CONTEST_ID,
                election(),
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                0));
    }

    @Test
    void addCandidateInitializesPendingCandidateAndAssociatesWithContest() {
        Contest contest = contest();

        Candidate candidate = contest.addCandidate(CANDIDATE_ID, "Kim Candidate", "Independent");

        assertEquals(CANDIDATE_ID, candidate.getId());
        assertSame(contest, candidate.getContest());
        assertEquals("Kim Candidate", candidate.getName());
        assertEquals("Independent", candidate.getPartyAffiliation());
        assertEquals(CandidateStatus.PENDING, candidate.getCandidateStatus());
        assertEquals(List.of(candidate), contest.getCandidates());
    }

    @Test
    void candidateRejectsInvalidRequiredFields() {
        Contest contest = contest();

        assertThrows(NullPointerException.class, () -> contest.addCandidate(null, "Kim Candidate", "Independent"));
        assertThrows(IllegalArgumentException.class, () -> contest.addCandidate(CANDIDATE_ID, "", "Independent"));
        assertThrows(IllegalArgumentException.class, () -> contest.addCandidate(CANDIDATE_ID, "Kim Candidate", " "));
    }

    @Test
    void approveCandidateMovesPendingCandidateToApprovedAndRecordsEvent() {
        Contest contest = contest();
        Candidate candidate = contest.addCandidate(CANDIDATE_ID, "Kim Candidate", "Independent");

        candidate.approve();

        assertEquals(CandidateStatus.APPROVED, candidate.getCandidateStatus());
        CandidateApprovedEvent event = assertInstanceOf(CandidateApprovedEvent.class, candidate.pullDomainEvents().getFirst());
        assertEquals(CANDIDATE_ID, event.candidateId());
        assertEquals(CONTEST_ID, event.contestId());
        assertEquals(ELECTION_ID, event.electionId());
        assertNotNull(event.occurredAt());
    }

    @Test
    void withdrawCandidateMovesCandidateToWithdrawnAndRecordsEvent() {
        Contest contest = contest();
        Candidate candidate = contest.addCandidate(CANDIDATE_ID, "Kim Candidate", "Independent");

        candidate.withdraw();

        assertEquals(CandidateStatus.WITHDRAWN, candidate.getCandidateStatus());
        CandidateWithdrawnEvent event = assertInstanceOf(CandidateWithdrawnEvent.class, candidate.pullDomainEvents().getFirst());
        assertEquals(CANDIDATE_ID, event.candidateId());
        assertEquals(CONTEST_ID, event.contestId());
        assertEquals(ELECTION_ID, event.electionId());
        assertNotNull(event.occurredAt());
    }

    @Test
    void pullDomainEventsClearsCandidateEvents() {
        Candidate candidate = contest().addCandidate(CANDIDATE_ID, "Kim Candidate", "Independent");

        candidate.approve();

        assertEquals(1, candidate.pullDomainEvents().size());
        assertTrue(candidate.pullDomainEvents().isEmpty());
    }

    @Test
    void candidateRejectsInvalidLifecycleTransitions() {
        Candidate candidate = contest().addCandidate(CANDIDATE_ID, "Kim Candidate", "Independent");

        candidate.approve();
        candidate.pullDomainEvents();

        CandidateStateException exception = assertThrows(CandidateStateException.class, candidate::withdraw);
        assertEquals("MIR-CANDIDATE-STATE-001", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("APPROVED"));
        assertTrue(exception.getMessage().contains("WITHDRAWN"));
        assertEquals(CandidateStatus.APPROVED, candidate.getCandidateStatus());
        assertTrue(candidate.pullDomainEvents().isEmpty());
    }

    @Test
    void candidateStatusAllowsOnlyExpectedTransitions() {
        for (CandidateStatus source : CandidateStatus.values()) {
            for (CandidateStatus target : CandidateStatus.values()) {
                boolean expected = switch (source) {
                    case PENDING -> target == CandidateStatus.APPROVED
                            || target == CandidateStatus.WITHDRAWN
                            || target == CandidateStatus.DISQUALIFIED;
                    case APPROVED, WITHDRAWN, DISQUALIFIED -> false;
                };

                assertEquals(expected, source.canTransitionTo(target), source + " -> " + target);
            }
        }
    }

    private static Contest contest() {
        return Contest.create(
                CONTEST_ID,
                election(),
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1);
    }

    private static Election election() {
        Election election = Election.create(
                ELECTION_ID,
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                LocalDate.of(2026, 6, 3),
                "KR",
                "ext-kr");
        election.pullDomainEvents();
        return election;
    }
}
