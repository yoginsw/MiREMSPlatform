package io.mirems.core.bpmn.tabulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.election.ElectionStatus;
import io.mirems.core.domain.result.TabulationReport;
import io.mirems.core.domain.result.TabulationReportRepository;
import io.mirems.core.domain.result.VotingResult;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BallotTabulationProcessServiceTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4b90-0000-7000-8000-000000000001");
    private static final UUID REPORT_ID = UUID.fromString("018f4b90-0000-7000-8000-000000000002");
    private static final UUID CONTEST_ONE = UUID.fromString("018f4b90-0000-7000-8000-000000000003");
    private static final UUID CONTEST_TWO = UUID.fromString("018f4b90-0000-7000-8000-000000000004");
    private static final UUID CANDIDATE_A = UUID.fromString("018f4b90-0000-7000-8000-000000000005");
    private static final UUID CANDIDATE_B = UUID.fromString("018f4b90-0000-7000-8000-000000000006");
    private static final UUID CANDIDATE_C = UUID.fromString("018f4b90-0000-7000-8000-000000000007");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-03T21:00:00Z");

    @Test
    void tabulatesOneHundredVotingResultsLocksReportAndPublishesWhenEnabled() {
        InMemoryVotingResultLoader loader = new InMemoryVotingResultLoader(sampleVotingResults());
        InMemoryReportRepository reports = new InMemoryReportRepository();
        BallotTabulationProcessService service = service(loader, reports);

        BallotTabulationResult result = service.tabulate(new BallotTabulationRequest(
                REPORT_ID,
                ELECTION_ID,
                ElectionStatus.CLOSED,
                "TABULATION_OFFICER",
                true));

        assertTrue(result.completed());
        assertTrue(result.published());
        assertNotNull(result.completedEvent());
        assertNotNull(result.report().getHash());
        assertTrue(result.report().verifyHash());
        assertEquals(REPORT_ID, reports.savedReport.getId());
        assertEquals(100, result.report().totalBallotsCounted());
        assertEquals(25, result.report().getContestTallies().get(CONTEST_ONE).candidateTallies().get(CANDIDATE_A));
        assertEquals(25, result.report().getContestTallies().get(CONTEST_ONE).candidateTallies().get(CANDIDATE_B));
        assertEquals(50, result.report().getContestTallies().get(CONTEST_TWO).candidateTallies().get(CANDIDATE_C));
        assertEquals(result.report().getHash(), result.completedEvent().reportHash());
    }

    @Test
    void createsLockedReportWithoutPublishingWhenElectionIsNotPublicResults() {
        BallotTabulationProcessService service = service(
                new InMemoryVotingResultLoader(sampleVotingResults()),
                new InMemoryReportRepository());

        BallotTabulationResult result = service.tabulate(new BallotTabulationRequest(
                REPORT_ID,
                ELECTION_ID,
                ElectionStatus.CLOSED,
                "TABULATION_OFFICER",
                false));

        assertTrue(result.completed());
        assertFalse(result.published());
        assertFalse(result.report().isPublished());
        assertTrue(result.report().isLocked());
    }

    @Test
    void rejectsTabulationBeforeElectionIsClosed() {
        BallotTabulationProcessService service = service(
                new InMemoryVotingResultLoader(sampleVotingResults()),
                new InMemoryReportRepository());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.tabulate(new BallotTabulationRequest(
                REPORT_ID,
                ELECTION_ID,
                ElectionStatus.ACTIVE,
                "TABULATION_OFFICER",
                true)));

        assertEquals("Election must be CLOSED before tabulation", exception.getMessage());
    }

    @Test
    void requiresTabulationOfficerReviewBeforeLockingReport() {
        BallotTabulationProcessService service = service(
                new InMemoryVotingResultLoader(sampleVotingResults()),
                new InMemoryReportRepository());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.tabulate(new BallotTabulationRequest(
                REPORT_ID,
                ELECTION_ID,
                ElectionStatus.CLOSED,
                "ELECTION_ADMIN",
                true)));

        assertEquals("TABULATION_OFFICER review is required", exception.getMessage());
    }

    private static BallotTabulationProcessService service(
            VotingResultLoader loader,
            TabulationReportRepository reports) {
        Clock clock = Clock.fixed(Instant.parse("2026-06-03T21:00:00Z"), ZoneOffset.UTC);
        return new BallotTabulationProcessService(loader, reports, clock);
    }

    private static List<VotingResult> sampleVotingResults() {
        List<VotingResult> results = new ArrayList<>();
        for (int index = 0; index < 25; index++) {
            results.add(votingResult(CONTEST_ONE, CANDIDATE_A));
            results.add(votingResult(CONTEST_ONE, CANDIDATE_B));
        }
        for (int index = 0; index < 50; index++) {
            results.add(votingResult(CONTEST_TWO, CANDIDATE_C));
        }
        return results;
    }

    private static VotingResult votingResult(UUID contestId, UUID candidateId) {
        Contest contest = mock(Contest.class);
        when(contest.getId()).thenReturn(contestId);
        VotingResult result = mock(VotingResult.class);
        when(result.getContest()).thenReturn(contest);
        when(result.getSelectedCandidateIds()).thenReturn(List.of(candidateId));
        return result;
    }

    private record InMemoryVotingResultLoader(List<VotingResult> results) implements VotingResultLoader {
        @Override
        public List<VotingResult> loadCommittedResults(UUID electionId) {
            assertEquals(ELECTION_ID, electionId);
            return List.copyOf(results);
        }
    }

    private static class InMemoryReportRepository implements TabulationReportRepository {
        private TabulationReport savedReport;

        @Override
        public TabulationReport save(TabulationReport report) {
            this.savedReport = report;
            return report;
        }

        @Override
        public Optional<TabulationReport> findById(UUID id) {
            return Optional.ofNullable(savedReport).filter(report -> report.getId().equals(id));
        }

        @Override
        public Optional<TabulationReport> findByElectionId(UUID electionId) {
            return Optional.ofNullable(savedReport).filter(report -> report.getElectionId().equals(electionId));
        }
    }
}
