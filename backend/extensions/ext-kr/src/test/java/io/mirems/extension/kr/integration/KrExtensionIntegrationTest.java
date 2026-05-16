package io.mirems.extension.kr.integration;

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
import io.mirems.core.domain.voting.VotingSessionOpeningContext;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.extension.kr.KrElectionType;
import io.mirems.extension.kr.earlyvoting.KrEarlyVotingPolicy;
import io.mirems.extension.kr.earlyvoting.KrEarlyVotingValidationException;
import io.mirems.extension.kr.proportional.KrDhondtSeatAllocationDecisionService;
import io.mirems.extension.kr.proportional.KrDhondtSeatAllocationRequest;
import io.mirems.extension.kr.proportional.KrPartySeatAllocation;
import io.mirems.extension.kr.proportional.KrPartyVote;
import io.mirems.extension.kr.rules.KrCampaignPeriodDecisionService;
import io.mirems.extension.kr.rules.KrCampaignPeriodRequest;
import io.mirems.extension.kr.rules.KrCandidateEligibilityDecisionService;
import io.mirems.extension.kr.rules.KrCandidateEligibilityRequest;
import io.mirems.extension.kr.rules.KrCitizenshipStatus;
import io.mirems.extension.kr.rules.KrVoterEligibilityDecisionService;
import io.mirems.extension.kr.rules.KrVoterEligibilityRequest;
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

class KrExtensionIntegrationTest {
    private static final LocalDate ELECTION_DAY = LocalDate.of(2028, 4, 12);
    private static final OffsetDateTime EARLY_VOTING_OPENED_AT = ELECTION_DAY.minusDays(5).atTime(9, 0).atOffset(ZoneOffset.UTC);
    private static final int VOTE_SIMULATION_SIZE = 10_000;
    private static final Duration P95_TARGET = Duration.ofMillis(100);

    private final KrVoterEligibilityDecisionService voterEligibility = new KrVoterEligibilityDecisionService();
    private final KrCandidateEligibilityDecisionService candidateEligibility = new KrCandidateEligibilityDecisionService();
    private final KrCampaignPeriodDecisionService campaignPeriod = new KrCampaignPeriodDecisionService();
    private final KrEarlyVotingPolicy earlyVotingPolicy = new KrEarlyVotingPolicy();
    private final KrDhondtSeatAllocationDecisionService seatAllocation = new KrDhondtSeatAllocationDecisionService();
    private final PiiEncryptionService encryptionService = new PiiEncryptionService("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Test
    void endToEndKrParliamentaryElectionRunsEarlyVotingAndProportionalTabulation() {
        Election election = krParliamentaryElection();
        Contest districtContest = Contest.create(id("contest-district"), election, ContestType.CANDIDATE_CHOICE, "서울 종로구 국회의원", 1, 1);
        Contest partyListContest = Contest.create(id("contest-party-list"), election, ContestType.CANDIDATE_CHOICE, "비례대표 정당명부", 6, 1);
        Candidate districtCandidate = districtContest.addCandidate(id("candidate-district"), "김미래", "시민당");
        Candidate citizenParty = partyListContest.addCandidate(id("party-citizen"), "시민당 비례명부", "시민당");
        Candidate greenParty = partyListContest.addCandidate(id("party-green"), "녹색당 비례명부", "녹색당");
        Candidate laborParty = partyListContest.addCandidate(id("party-labor"), "노동당 비례명부", "노동당");
        approveAll(districtCandidate, citizenParty, greenParty, laborParty);
        BallotStyle ballotStyle = ballotStyle(election, districtContest, partyListContest);
        VoterRecord voter = activeVoter("SEOUL-JONGNO-0001", election);

        assertThat(voterEligibility.evaluate(new KrVoterEligibilityRequest(
                        18, KrCitizenshipStatus.CITIZEN, false, KrElectionType.NATIONAL_ASSEMBLY_ELECTION)).eligible())
                .isTrue();
        earlyVotingPolicy.validate(new VotingSessionOpeningContext(
                voter,
                election,
                ballotStyle,
                VotingMethod.EARLY_VOTING,
                EARLY_VOTING_OPENED_AT,
                "SEOUL-JONGNO",
                "BUSAN-HAEUNDAE"));

        VotingSession session = voter.openVotingSession(
                id("session-early-vote"),
                election,
                ballotStyle,
                "tablet-early-001",
                EARLY_VOTING_OPENED_AT,
                VotingMethod.EARLY_VOTING);
        VotingResult districtVote = VotingResult.create(
                id("result-district"),
                session,
                districtContest,
                List.of(districtCandidate.getId()),
                EARLY_VOTING_OPENED_AT.plusMinutes(4));
        VotingResult partyListVote = VotingResult.create(
                id("result-party-list"),
                session,
                partyListContest,
                List.of(citizenParty.getId()),
                EARLY_VOTING_OPENED_AT.plusMinutes(4));
        districtVote.computeHashBeforePersist();
        partyListVote.computeHashBeforePersist();
        session.cast(EARLY_VOTING_OPENED_AT.plusMinutes(5));

        assertThat(session.getVotingMethod()).isEqualTo(VotingMethod.EARLY_VOTING);
        assertThat(session.getSessionStatus().name()).isEqualTo("CAST");
        assertThat(districtVote.verifyHash()).isTrue();
        assertThat(partyListVote.verifyHash()).isTrue();
        assertThat(seatAllocation.allocate(new KrDhondtSeatAllocationRequest(6, List.of(
                        new KrPartyVote(citizenParty.getId().toString(), "시민당", 120_000),
                        new KrPartyVote(greenParty.getId().toString(), "녹색당", 80_000),
                        new KrPartyVote(laborParty.getId().toString(), "노동당", 40_000))))
                .allocations())
                .extracting(KrPartySeatAllocation::partyName, KrPartySeatAllocation::allocatedSeats)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("시민당", 3),
                        org.assertj.core.groups.Tuple.tuple("녹색당", 2),
                        org.assertj.core.groups.Tuple.tuple("노동당", 1));
    }

    @Test
    void integrationRejectsIneligibleVotersCandidatesAndIllegalVotingWindows() {
        Election election = krParliamentaryElection();
        BallotStyle ballotStyle = ballotStyle(election, Contest.create(id("contest-law"), election, ContestType.CANDIDATE_CHOICE, "법정 제약 검증", 1, 1));
        VoterRecord voter = activeVoter("SEOUL-JONGNO-0002", election);

        assertThat(voterEligibility.evaluate(new KrVoterEligibilityRequest(
                        17, KrCitizenshipStatus.CITIZEN, false, KrElectionType.NATIONAL_ASSEMBLY_ELECTION)).eligible())
                .isFalse();
        assertThat(voterEligibility.evaluate(new KrVoterEligibilityRequest(
                        38, KrCitizenshipStatus.FOREIGN_PERMANENT_RESIDENT, true, KrElectionType.NATIONAL_ASSEMBLY_ELECTION)).reason())
                .isEqualTo("national elections require Korean citizenship");
        assertThat(candidateEligibility.evaluate(new KrCandidateEligibilityRequest(
                        45, KrCitizenshipStatus.CITIZEN, true, KrElectionType.NATIONAL_ASSEMBLY_ELECTION)).eligible())
                .isFalse();
        assertThat(candidateEligibility.evaluate(new KrCandidateEligibilityRequest(
                        17, KrCitizenshipStatus.CITIZEN, false, KrElectionType.NATIONAL_ASSEMBLY_ELECTION)).reason())
                .isEqualTo("non-presidential candidates must be at least 18");
        assertThat(campaignPeriod.evaluate(new KrCampaignPeriodRequest(KrElectionType.NATIONAL_ASSEMBLY_ELECTION, 15)).allowed())
                .isFalse();
        assertThat(campaignPeriod.evaluate(new KrCampaignPeriodRequest(KrElectionType.NATIONAL_ASSEMBLY_ELECTION, 14)).allowed())
                .isTrue();

        assertThatThrownBy(() -> earlyVotingPolicy.validate(new VotingSessionOpeningContext(
                        voter,
                        election,
                        ballotStyle,
                        VotingMethod.EARLY_VOTING,
                        ELECTION_DAY.minusDays(3).atTime(9, 0).atOffset(ZoneOffset.UTC),
                        "SEOUL-JONGNO",
                        "BUSAN-HAEUNDAE")))
                .isInstanceOf(KrEarlyVotingValidationException.class)
                .hasMessageContaining("D-5 to D-4");
        assertThatThrownBy(() -> earlyVotingPolicy.validate(new VotingSessionOpeningContext(
                        voter,
                        election,
                        ballotStyle,
                        VotingMethod.ELECTION_DAY,
                        ELECTION_DAY.atTime(9, 0).atOffset(ZoneOffset.UTC),
                        "SEOUL-JONGNO",
                        "BUSAN-HAEUNDAE")))
                .isInstanceOf(KrEarlyVotingValidationException.class)
                .hasMessageContaining("home district polling station");
    }

    @Test
    void tenThousandVoteSimulationStaysBelowP95PerformanceTarget() {
        Election election = krParliamentaryElection();
        Contest contest = Contest.create(id("contest-performance"), election, ContestType.CANDIDATE_CHOICE, "비례대표 성능 검증", 3, 1);
        Candidate party = contest.addCandidate(id("party-performance"), "시민당 비례명부", "시민당");
        party.approve();
        BallotStyle ballotStyle = ballotStyle(election, contest);
        List<Long> elapsedNanos = new ArrayList<>(VOTE_SIMULATION_SIZE);

        for (int index = 0; index < VOTE_SIMULATION_SIZE; index++) {
            VoterRecord voter = activeVoter("SEOUL-PERF-%05d".formatted(index), election);
            OffsetDateTime openedAt = EARLY_VOTING_OPENED_AT.plusSeconds(index % 3_600);
            long started = System.nanoTime();
            earlyVotingPolicy.validate(new VotingSessionOpeningContext(
                    voter,
                    election,
                    ballotStyle,
                    VotingMethod.EARLY_VOTING,
                    openedAt,
                    "SEOUL-JONGNO",
                    "INCHEON-BUPYEONG"));
            VotingSession session = voter.openVotingSession(
                    id("session-performance-" + index),
                    election,
                    ballotStyle,
                    "tablet-performance",
                    openedAt,
                    VotingMethod.EARLY_VOTING);
            VotingResult vote = VotingResult.create(
                    id("vote-performance-" + index),
                    session,
                    contest,
                    List.of(party.getId()),
                    openedAt.plusMinutes(1));
            vote.computeHashBeforePersist();
            session.cast(openedAt.plusMinutes(2));
            elapsedNanos.add(System.nanoTime() - started);
        }

        Duration p95 = Duration.ofNanos(percentile(elapsedNanos, 95));
        assertThat(p95).isLessThanOrEqualTo(P95_TARGET);
    }

    private Election krParliamentaryElection() {
        return Election.create(
                id("election-kr-2028"),
                "제22대 국회의원선거",
                ElectionType.PARLIAMENTARY,
                "KR-SEOUL",
                ELECTION_DAY,
                "KR",
                "ext-kr");
    }

    private BallotStyle ballotStyle(Election election, Contest... contests) {
        Ballot ballot = Ballot.create(id("ballot-" + contests[0].getId()), election);
        for (int index = 0; index < contests.length; index++) {
            ballot.addContest(contests[index], index + 1, contests[index].getName());
        }
        return ballot.addStyle(id("style-" + contests[0].getId()), "KR-SEOUL-KO", "SEOUL-JONGNO", "ko", Set.of(AccessibilityFeature.LARGE_TEXT));
    }

    private VoterRecord activeVoter(String externalVoterId, Election election) {
        return VoterRecord.create(
                id("voter-" + externalVoterId),
                externalVoterId,
                Set.of(election.getId()),
                RegistrationStatus.ACTIVE,
                encryptionService);
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
