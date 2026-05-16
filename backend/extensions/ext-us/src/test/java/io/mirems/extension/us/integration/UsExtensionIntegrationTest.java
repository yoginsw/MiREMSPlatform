package io.mirems.extension.us.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Candidate;
import io.mirems.core.domain.contest.CandidateStatus;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.result.VotingResult;
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.VotingMethod;
import io.mirems.core.domain.voting.VotingSession;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.extension.us.absentee.UsAbsenteeBallotRecord;
import io.mirems.extension.us.absentee.UsAbsenteeBallotStatus;
import io.mirems.extension.us.absentee.UsAbsenteeBallotTrackingService;
import io.mirems.extension.us.provisional.UsProvisionalBallot;
import io.mirems.extension.us.provisional.UsProvisionalBallotStatus;
import io.mirems.extension.us.provisional.UsProvisionalBallotWorkflowService;
import io.mirems.extension.us.rcv.UsInstantRunoffRequest;
import io.mirems.extension.us.rcv.UsInstantRunoffTabulationService;
import io.mirems.extension.us.rcv.UsRankedChoiceBallot;
import io.mirems.extension.us.rules.UsAbsenteeBallotRequest;
import io.mirems.extension.us.rules.UsAbsenteeVoterCategory;
import io.mirems.extension.us.rules.UsCitizenshipStatus;
import io.mirems.extension.us.rules.UsElectionType;
import io.mirems.extension.us.rules.UsIdVerificationStatus;
import io.mirems.extension.us.rules.UsVoterEligibilityDecisionService;
import io.mirems.extension.us.rules.UsVoterEligibilityRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsExtensionIntegrationTest {
    private static final LocalDate ELECTION_DAY = LocalDate.of(2028, 11, 7);
    private static final OffsetDateTime OPENED_AT = ELECTION_DAY.atTime(9, 0).atOffset(ZoneOffset.UTC);
    private static final int VOTE_SIMULATION_SIZE = 10_000;
    private static final Duration SIMULATION_SMOKE_TARGET = Duration.ofSeconds(5);

    private final UsVoterEligibilityDecisionService voterEligibility = new UsVoterEligibilityDecisionService();
    private final UsProvisionalBallotWorkflowService provisionalBallots = new UsProvisionalBallotWorkflowService();
    private final UsAbsenteeBallotTrackingService absenteeTracking = new UsAbsenteeBallotTrackingService();
    private final UsInstantRunoffTabulationService rcvTabulation = new UsInstantRunoffTabulationService();
    private final PiiEncryptionService encryptionService = new PiiEncryptionService("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void endToEndUsGeneralElectionRunsProvisionalAbsenteeAndRcvTabulation() {
        Election election = usGeneralElection();
        Contest mayorContest = Contest.create(id("contest-mayor"), election, ContestType.RANKED_CHOICE, "San Francisco Mayor", 1, 1);
        Candidate alice = mayorContest.addCandidate(id("candidate-alice"), "Alice Rivera", "Forward");
        Candidate bob = mayorContest.addCandidate(id("candidate-bob"), "Bob Chen", "Civic");
        Candidate carol = mayorContest.addCandidate(id("candidate-carol"), "Carol Smith", "Neighborhood");
        approveAll(alice, bob, carol);
        BallotStyle ballotStyle = ballotStyle(election, mayorContest);
        VoterRecord voter = activeVoter("US-CA-06075-0001", election);

        UsVoterEligibilityRequest eligibilityRequest = new UsVoterEligibilityRequest(
                UsCitizenshipStatus.CITIZEN,
                LocalDate.of(1990, 3, 12),
                ELECTION_DAY,
                ELECTION_DAY,
                UsElectionType.GENERAL_ELECTION,
                "CA",
                UsIdVerificationStatus.UNVERIFIED_HAVA_ID,
                true);
        assertThat(voterEligibility.evaluate(eligibilityRequest).provisionalBallotRequired()).isTrue();
        UsProvisionalBallot provisional = provisionalBallots.createFromEligibility(
                voter.getId(), election.getId(), id("provisional-1"), eligibilityRequest, OPENED_AT);
        assertThat(provisional.status()).isEqualTo(UsProvisionalBallotStatus.PENDING_REVIEW);

        UsAbsenteeBallotRecord absentee = absenteeTracking.requestBallot(
                voter.getId(),
                election.getId(),
                new UsAbsenteeBallotRequest(UsAbsenteeVoterCategory.MILITARY, ELECTION_DAY.minusDays(35), ELECTION_DAY, "CA", false),
                OPENED_AT.minusDays(35));
        assertThat(absenteeTracking.markSent(absentee, OPENED_AT.minusDays(34), "email").status()).isEqualTo(UsAbsenteeBallotStatus.SENT);

        VotingSession session = voter.openVotingSession(
                id("session-us-rcv"), election, ballotStyle, "tablet-us-001", OPENED_AT, VotingMethod.ELECTION_DAY);
        VotingResult vote = VotingResult.create(id("result-us-rcv"), session, mayorContest, List.of(alice.getId()), OPENED_AT.plusMinutes(4));
        vote.computeHashBeforePersist();
        session.cast(OPENED_AT.plusMinutes(5));

        assertThat(session.getSessionStatus().name()).isEqualTo("CAST");
        assertThat(vote.verifyHash()).isTrue();
        assertThat(rcvTabulation.tabulate(new UsInstantRunoffRequest(mayorContest.getId(), Set.of(alice.getId(), bob.getId(), carol.getId()), List.of(
                        UsRankedChoiceBallot.cast(mayorContest.getId(), List.of(alice.getId(), bob.getId(), carol.getId()), Set.of(alice.getId(), bob.getId(), carol.getId())),
                        UsRankedChoiceBallot.cast(mayorContest.getId(), List.of(bob.getId(), alice.getId(), carol.getId()), Set.of(alice.getId(), bob.getId(), carol.getId())),
                        UsRankedChoiceBallot.cast(mayorContest.getId(), List.of(bob.getId(), carol.getId(), alice.getId()), Set.of(alice.getId(), bob.getId(), carol.getId()))))).winnerCandidateId())
                .contains(bob.getId());
    }

    @Test
    void integrationRejectsNonCitizenInvalidAbsenteeAndDuplicateRcvRanks() {
        assertThat(voterEligibility.evaluate(new UsVoterEligibilityRequest(
                        UsCitizenshipStatus.NON_CITIZEN,
                        LocalDate.of(1990, 1, 1),
                        ELECTION_DAY,
                        ELECTION_DAY,
                        UsElectionType.GENERAL_ELECTION,
                        "CA",
                        UsIdVerificationStatus.VERIFIED,
                        true)).eligible())
                .isFalse();
        assertThatThrownBy(() -> absenteeTracking.requestBallot(
                        id("voter-invalid-absentee"),
                        id("election-invalid-absentee"),
                        new UsAbsenteeBallotRequest(UsAbsenteeVoterCategory.NOT_ABSENTEE_ELIGIBLE, ELECTION_DAY.minusDays(10), ELECTION_DAY, "CA", false),
                        OPENED_AT.minusDays(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not absentee eligible");
        assertThatThrownBy(() -> new UsRankedChoiceBallot(id("contest-duplicate"), List.of(id("candidate-a"), id("candidate-a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicates");
    }

    @Test
    void tenThousandVoteSimulationCompletesWithinSmokeTargetAndRecordsP95() {
        Election election = usGeneralElection();
        Contest contest = Contest.create(id("contest-performance-us"), election, ContestType.CANDIDATE_CHOICE, "Performance Contest", 1, 1);
        Candidate candidate = contest.addCandidate(id("candidate-performance-us"), "Pat Taylor", "Civic");
        candidate.approve();
        BallotStyle ballotStyle = ballotStyle(election, contest);
        List<Long> elapsedNanos = new ArrayList<>(VOTE_SIMULATION_SIZE);
        long simulationStarted = System.nanoTime();

        for (int index = 0; index < VOTE_SIMULATION_SIZE; index++) {
            VoterRecord voter = activeVoter("US-PERF-%05d".formatted(index), election);
            long started = System.nanoTime();
            VotingSession session = voter.openVotingSession(id("session-us-performance-" + index), election, ballotStyle, "tablet-us-performance", OPENED_AT, VotingMethod.ELECTION_DAY);
            VotingResult vote = VotingResult.create(id("vote-us-performance-" + index), session, contest, List.of(candidate.getId()), OPENED_AT.plusMinutes(1));
            vote.computeHashBeforePersist();
            session.cast(OPENED_AT.plusMinutes(2));
            elapsedNanos.add(System.nanoTime() - started);
        }

        Duration totalElapsed = Duration.ofNanos(System.nanoTime() - simulationStarted);
        Duration p95Elapsed = Duration.ofNanos(percentile(elapsedNanos, 95));
        assertThat(totalElapsed).isLessThanOrEqualTo(SIMULATION_SMOKE_TARGET);
        assertThat(p95Elapsed).isPositive();
    }

    private Election usGeneralElection() {
        return Election.create(id("election-us-2028"), "2028 US General Election", ElectionType.PRESIDENTIAL, "US-CA", ELECTION_DAY, "US", "ext-us");
    }

    private BallotStyle ballotStyle(Election election, Contest... contests) {
        Ballot ballot = Ballot.create(id("ballot-us-" + contests[0].getId()), election);
        for (int index = 0; index < contests.length; index++) {
            ballot.addContest(contests[index], index + 1, contests[index].getName());
        }
        return ballot.addStyle(id("style-us-" + contests[0].getId()), "US-CA-EN", "06075", "en", Set.of(AccessibilityFeature.AUDIO));
    }

    private VoterRecord activeVoter(String externalVoterId, Election election) {
        return VoterRecord.create(id("voter-" + externalVoterId), externalVoterId, Set.of(election.getId()), RegistrationStatus.ACTIVE, encryptionService);
    }

    private static void approveAll(Candidate... candidates) {
        for (Candidate candidate : candidates) {
            candidate.approve();
            assertThat(candidate.getCandidateStatus()).isEqualTo(CandidateStatus.APPROVED);
        }
    }

    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static long percentile(List<Long> values, int percentile) {
        List<Long> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
