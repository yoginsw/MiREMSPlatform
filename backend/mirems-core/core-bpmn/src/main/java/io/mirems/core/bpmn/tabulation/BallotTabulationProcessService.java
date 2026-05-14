package io.mirems.core.bpmn.tabulation;

import io.mirems.core.domain.election.ElectionStatus;
import io.mirems.core.domain.result.ContestTally;
import io.mirems.core.domain.result.TabulationCompletedEvent;
import io.mirems.core.domain.result.TabulationReport;
import io.mirems.core.domain.result.TabulationReportRepository;
import io.mirems.core.domain.result.VotingResult;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({VotingResultLoader.class, TabulationReportRepository.class})
public class BallotTabulationProcessService {
    private static final String REQUIRED_REVIEW_ROLE = "TABULATION_OFFICER";

    private final VotingResultLoader votingResultLoader;
    private final TabulationReportRepository tabulationReportRepository;
    private final Clock clock;

    @Autowired
    public BallotTabulationProcessService(
            VotingResultLoader votingResultLoader,
            TabulationReportRepository tabulationReportRepository) {
        this(votingResultLoader, tabulationReportRepository, Clock.systemUTC());
    }

    public BallotTabulationProcessService(
            VotingResultLoader votingResultLoader,
            TabulationReportRepository tabulationReportRepository,
            Clock clock) {
        this.votingResultLoader = Objects.requireNonNull(votingResultLoader, "votingResultLoader is required");
        this.tabulationReportRepository = Objects.requireNonNull(
                tabulationReportRepository,
                "tabulationReportRepository is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public BallotTabulationResult tabulate(BallotTabulationRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (request.electionStatus() != ElectionStatus.CLOSED) {
            throw new IllegalStateException("Election must be CLOSED before tabulation");
        }
        if (!REQUIRED_REVIEW_ROLE.equals(request.reviewerRole())) {
            throw new IllegalArgumentException("TABULATION_OFFICER review is required");
        }

        List<VotingResult> votingResults = votingResultLoader.loadCommittedResults(request.electionId());
        TabulationReport report = TabulationReport.draft(
                request.reportId(),
                request.electionId(),
                aggregate(votingResults),
                OffsetDateTime.now(clock));
        TabulationCompletedEvent completedEvent = report.lock(OffsetDateTime.now(clock));
        if (request.publicResults()) {
            report.markPublished();
        }
        TabulationReport savedReport = tabulationReportRepository.save(report);
        return new BallotTabulationResult(true, savedReport.isPublished(), savedReport, completedEvent);
    }

    private Map<UUID, ContestTally> aggregate(List<VotingResult> votingResults) {
        Objects.requireNonNull(votingResults, "votingResults is required");
        if (votingResults.isEmpty()) {
            throw new IllegalArgumentException("votingResults must not be empty");
        }

        Map<UUID, Map<UUID, Integer>> candidateCountsByContest = new HashMap<>();
        Map<UUID, Integer> ballotsByContest = new HashMap<>();
        for (VotingResult result : votingResults) {
            UUID contestId = result.getContest().getId();
            ballotsByContest.merge(contestId, 1, Integer::sum);
            Map<UUID, Integer> candidateCounts = candidateCountsByContest.computeIfAbsent(contestId, ignored -> new HashMap<>());
            result.getSelectedCandidateIds().forEach(candidateId -> candidateCounts.merge(candidateId, 1, Integer::sum));
        }

        Map<UUID, ContestTally> tallies = new HashMap<>();
        ballotsByContest.forEach((contestId, ballotsCounted) -> tallies.put(
                contestId,
                new ContestTally(contestId, candidateCountsByContest.get(contestId), ballotsCounted)));
        return Map.copyOf(tallies);
    }
}
