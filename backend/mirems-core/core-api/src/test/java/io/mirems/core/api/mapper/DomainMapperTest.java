package io.mirems.core.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.api.dto.BallotCreateRequest;
import io.mirems.core.api.dto.BallotResponse;
import io.mirems.core.api.dto.BallotStyleCreateRequest;
import io.mirems.core.api.dto.CandidateCreateRequest;
import io.mirems.core.api.dto.CandidateResponse;
import io.mirems.core.api.dto.CastBallotRequest;
import io.mirems.core.api.dto.ContestCreateRequest;
import io.mirems.core.api.dto.ContestResponse;
import io.mirems.core.api.dto.ElectionCreateRequest;
import io.mirems.core.api.dto.ElectionResponse;
import io.mirems.core.api.dto.VoterRegistrationRequest;
import io.mirems.core.api.dto.VoterResponse;
import io.mirems.core.api.dto.VotingResultResponse;
import io.mirems.core.api.dto.VotingSessionOpenRequest;
import io.mirems.core.api.dto.VotingSessionResponse;
import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Candidate;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.result.VotingResult;
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.VotingSession;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class DomainMapperTest {
    private static final UUID ELECTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CONTEST_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CANDIDATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BALLOT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID BALLOT_STYLE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID VOTER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID SESSION_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID RESULT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private final ElectionMapper electionMapper = Mappers.getMapper(ElectionMapper.class);
    private final ContestMapper contestMapper = Mappers.getMapper(ContestMapper.class);
    private final CandidateMapper candidateMapper = Mappers.getMapper(CandidateMapper.class);
    private final BallotMapper ballotMapper = Mappers.getMapper(BallotMapper.class);
    private final VoterMapper voterMapper = Mappers.getMapper(VoterMapper.class);
    private final VotingSessionMapper votingSessionMapper = Mappers.getMapper(VotingSessionMapper.class);
    private final VotingResultMapper votingResultMapper = Mappers.getMapper(VotingResultMapper.class);

    @Test
    void mapsElectionToResponse() {
        Election election = election();

        ElectionResponse response = electionMapper.toResponse(election);

        assertThat(response.id()).isEqualTo(ELECTION_ID);
        assertThat(response.name()).isEqualTo("General Election");
        assertThat(response.electionType()).isEqualTo(ElectionType.PRESIDENTIAL);
        assertThat(response.jurisdiction()).isEqualTo("Seoul");
        assertThat(response.scheduledDate()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(response.electionStatus().name()).isEqualTo("DRAFT");
        assertThat(response.countryCode()).isEqualTo("KR");
        assertThat(response.extensionPackId()).isEqualTo("ext-kr");
    }

    @Test
    void mapsContestAndCandidateToResponseWithParentIds() {
        Contest contest = contest();
        Candidate candidate = contest.addCandidate(CANDIDATE_ID, "Kim Candidate", "Independent");

        ContestResponse contestResponse = contestMapper.toResponse(contest);
        CandidateResponse candidateResponse = candidateMapper.toResponse(candidate);

        assertThat(contestResponse.id()).isEqualTo(CONTEST_ID);
        assertThat(contestResponse.electionId()).isEqualTo(ELECTION_ID);
        assertThat(contestResponse.contestType()).isEqualTo(ContestType.CANDIDATE_CHOICE);
        assertThat(contestResponse.name()).isEqualTo("Mayor");
        assertThat(contestResponse.seats()).isEqualTo(1);
        assertThat(contestResponse.voteLimit()).isEqualTo(1);
        assertThat(candidateResponse.id()).isEqualTo(CANDIDATE_ID);
        assertThat(candidateResponse.contestId()).isEqualTo(CONTEST_ID);
        assertThat(candidateResponse.electionId()).isEqualTo(ELECTION_ID);
        assertThat(candidateResponse.name()).isEqualTo("Kim Candidate");
        assertThat(candidateResponse.partyAffiliation()).isEqualTo("Independent");
        assertThat(candidateResponse.candidateStatus().name()).isEqualTo("PENDING");
    }

    @Test
    void mapsBallotWithContestAndStyleSummaries() {
        Election election = election();
        Contest contest = Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
        Ballot ballot = Ballot.create(BALLOT_ID, election);
        ballot.addContest(contest, 1, "Mayor of Seoul");
        ballot.addStyle(BALLOT_STYLE_ID, "SEOUL-EN", "Seoul", "en", Set.of(AccessibilityFeature.AUDIO));
        ballot.activate();

        BallotResponse response = ballotMapper.toResponse(ballot);

        assertThat(response.id()).isEqualTo(BALLOT_ID);
        assertThat(response.electionId()).isEqualTo(ELECTION_ID);
        assertThat(response.ballotVersion()).isEqualTo(4);
        assertThat(response.active()).isTrue();
        assertThat(response.contests()).singleElement().satisfies(summary -> {
            assertThat(summary.contestId()).isEqualTo(CONTEST_ID);
            assertThat(summary.displayOrder()).isEqualTo(1);
            assertThat(summary.presentationTitle()).isEqualTo("Mayor of Seoul");
        });
        assertThat(response.styles()).singleElement().satisfies(style -> {
            assertThat(style.id()).isEqualTo(BALLOT_STYLE_ID);
            assertThat(style.ballotId()).isEqualTo(BALLOT_ID);
            assertThat(style.styleCode()).isEqualTo("SEOUL-EN");
            assertThat(style.accessibilityFeatures()).containsExactly(AccessibilityFeature.AUDIO);
        });
    }

    @Test
    void mapsVoterWithoutExposingRawOrEncryptedExternalVoterId() {
        VoterRecord voter = voterRecord();

        VoterResponse response = voterMapper.toResponse(voter);

        assertThat(response.id()).isEqualTo(VOTER_ID);
        assertThat(response.eligibleElections()).containsExactly(ELECTION_ID);
        assertThat(response.registrationStatus()).isEqualTo(RegistrationStatus.ACTIVE);
        assertThat(recordComponentNames(VoterResponse.class))
                .doesNotContain("externalVoterId", "encryptedExternalVoterId");
        assertThat(response.toString()).doesNotContain("RAW-VOTER-001");
    }

    @Test
    void mapsVotingSessionWithoutVoterPii() {
        Election election = election();
        Ballot ballot = Ballot.create(BALLOT_ID, election);
        BallotStyle style = ballot.addStyle(BALLOT_STYLE_ID, "SEOUL-EN", "Seoul", "en", Set.of(AccessibilityFeature.AUDIO));
        VoterRecord voter = voterRecord();
        VotingSession session = voter.openVotingSession(
                SESSION_ID,
                election,
                style,
                "DEVICE-1",
                OffsetDateTime.parse("2026-06-03T09:00:00Z"));

        VotingSessionResponse response = votingSessionMapper.toResponse(session);

        assertThat(response.id()).isEqualTo(SESSION_ID);
        assertThat(response.voterRecordId()).isEqualTo(VOTER_ID);
        assertThat(response.electionId()).isEqualTo(ELECTION_ID);
        assertThat(response.ballotStyleId()).isEqualTo(BALLOT_STYLE_ID);
        assertThat(response.sessionStatus().name()).isEqualTo("OPENED");
        assertThat(response.deviceId()).isEqualTo("DEVICE-1");
        assertThat(response.toString()).doesNotContain("RAW-VOTER-001");
    }

    @Test
    void mapsVotingResultWithoutVoterPii() {
        Election election = election();
        Contest contest = Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
        Candidate candidate = contest.addCandidate(CANDIDATE_ID, "Kim Candidate", "Independent");
        Ballot ballot = Ballot.create(BALLOT_ID, election);
        BallotStyle style = ballot.addStyle(BALLOT_STYLE_ID, "SEOUL-EN", "Seoul", "en", Set.of(AccessibilityFeature.AUDIO));
        VoterRecord voter = voterRecord();
        VotingSession session = voter.openVotingSession(SESSION_ID, election, style, "DEVICE-1", OffsetDateTime.parse("2026-06-03T09:00:00Z"));
        VotingResult result = VotingResult.create(
                RESULT_ID,
                session,
                contest,
                List.of(candidate.getId()),
                OffsetDateTime.parse("2026-06-03T09:05:00Z"));
        result.computeHashBeforePersist();

        VotingResultResponse response = votingResultMapper.toResponse(result);

        assertThat(response.id()).isEqualTo(RESULT_ID);
        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
        assertThat(response.contestId()).isEqualTo(CONTEST_ID);
        assertThat(response.selectedCandidateIds()).containsExactly(CANDIDATE_ID);
        assertThat(response.hash()).hasSize(64);
        assertThat(response.toString()).doesNotContain("RAW-VOTER-001");
    }

    @Test
    void mappersReturnNullForNullDomainInput() {
        assertThat(electionMapper.toResponse(null)).isNull();
        assertThat(contestMapper.toResponse(null)).isNull();
        assertThat(candidateMapper.toResponse(null)).isNull();
        assertThat(ballotMapper.toResponse((Ballot) null)).isNull();
        assertThat(voterMapper.toResponse(null)).isNull();
        assertThat(votingSessionMapper.toResponse(null)).isNull();
        assertThat(votingResultMapper.toResponse(null)).isNull();
    }

    @Test
    void requestDtosCarryApiInputWithoutPiiInResponses() {
        ElectionCreateRequest electionRequest = new ElectionCreateRequest(
                "General Election", ElectionType.PRESIDENTIAL, "Seoul", LocalDate.of(2026, 6, 3), "KR", "ext-kr");
        ContestCreateRequest contestRequest = new ContestCreateRequest(
                ELECTION_ID, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
        CandidateCreateRequest candidateRequest = new CandidateCreateRequest(CONTEST_ID, "Kim Candidate", "Independent");
        BallotCreateRequest ballotRequest = new BallotCreateRequest(ELECTION_ID);
        BallotStyleCreateRequest styleRequest = new BallotStyleCreateRequest(
                BALLOT_ID, "SEOUL-EN", "Seoul", "en", Set.of(AccessibilityFeature.AUDIO));
        VoterRegistrationRequest voterRequest = new VoterRegistrationRequest(
                "RAW-VOTER-001", Set.of(ELECTION_ID), RegistrationStatus.ACTIVE);
        VotingSessionOpenRequest sessionRequest = new VotingSessionOpenRequest(
                VOTER_ID, ELECTION_ID, BALLOT_STYLE_ID, "DEVICE-1");
        CastBallotRequest castRequest = new CastBallotRequest(SESSION_ID, CONTEST_ID, List.of(CANDIDATE_ID));

        assertThat(electionRequest.name()).isEqualTo("General Election");
        assertThat(contestRequest.electionId()).isEqualTo(ELECTION_ID);
        assertThat(candidateRequest.contestId()).isEqualTo(CONTEST_ID);
        assertThat(ballotRequest.electionId()).isEqualTo(ELECTION_ID);
        assertThat(styleRequest.accessibilityFeatures()).containsExactly(AccessibilityFeature.AUDIO);
        assertThat(voterRequest.externalVoterId()).isEqualTo("RAW-VOTER-001");
        assertThat(sessionRequest.ballotStyleId()).isEqualTo(BALLOT_STYLE_ID);
        assertThat(castRequest.selectedCandidateIds()).containsExactly(CANDIDATE_ID);
    }

    @Test
    void responseDtosNeverExposeRawOrEncryptedExternalVoterIdComponents() {
        List<Class<? extends Record>> responseTypes = List.of(
                ElectionResponse.class,
                ContestResponse.class,
                CandidateResponse.class,
                BallotResponse.class,
                VoterResponse.class,
                VotingSessionResponse.class,
                VotingResultResponse.class);

        assertThat(responseTypes)
                .allSatisfy(type -> assertThat(recordComponentNames(type))
                        .doesNotContain("externalVoterId", "encryptedExternalVoterId"));
    }

    private static Election election() {
        return Election.create(
                ELECTION_ID,
                "General Election",
                ElectionType.PRESIDENTIAL,
                "Seoul",
                LocalDate.of(2026, 6, 3),
                "KR",
                "ext-kr");
    }

    private static Contest contest() {
        return Contest.create(CONTEST_ID, election(), ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
    }

    private static VoterRecord voterRecord() {
        return VoterRecord.create(
                VOTER_ID,
                "RAW-VOTER-001",
                Set.of(ELECTION_ID),
                RegistrationStatus.ACTIVE,
                new PiiEncryptionService(new byte[32]));
    }

    private static List<String> recordComponentNames(Class<? extends Record> recordType) {
        return List.of(recordType.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .toList();
    }
}
