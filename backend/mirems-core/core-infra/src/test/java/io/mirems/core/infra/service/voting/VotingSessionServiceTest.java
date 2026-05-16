package io.mirems.core.infra.service.voting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.result.VotingResult;
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.SessionStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.VotingSession;
import io.mirems.core.domain.voting.VotingSessionValidationException;
import io.mirems.core.domain.voting.VotingMethod;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.core.infra.persistence.ballot.SpringDataBallotStyleRepository;
import io.mirems.core.infra.persistence.contest.SpringDataContestRepository;
import io.mirems.core.infra.persistence.election.SpringDataElectionRepository;
import io.mirems.core.infra.persistence.result.SpringDataVotingResultJpaRepository;
import io.mirems.core.infra.persistence.voting.SpringDataVoterRecordRepository;
import io.mirems.core.infra.persistence.voting.SpringDataVotingSessionRepository;
import io.mirems.core.infra.service.election.TransactionalAuditEvent;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class VotingSessionServiceTest {
    private static final UUID SESSION_ID = UUID.fromString("018f4b81-1111-7111-8111-111111111111");
    private static final UUID RESULT_ID = UUID.fromString("018f4b81-2222-7222-8222-222222222222");
    private static final UUID VOTER_ID = UUID.fromString("018f4b81-3333-7333-8333-333333333333");
    private static final UUID ELECTION_ID = UUID.fromString("018f4b81-4444-7444-8444-444444444444");
    private static final UUID BALLOT_STYLE_ID = UUID.fromString("018f4b81-5555-7555-8555-555555555555");
    private static final UUID CONTEST_ID = UUID.fromString("018f4b81-6666-7666-8666-666666666666");
    private static final UUID CANDIDATE_ID = UUID.fromString("018f4b81-7777-7777-8777-777777777777");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-03T09:00:00Z");

    @Mock private SpringDataVoterRecordRepository voterRecordRepository;
    @Mock private SpringDataElectionRepository electionRepository;
    @Mock private SpringDataBallotStyleRepository ballotStyleRepository;
    @Mock private SpringDataVotingSessionRepository votingSessionRepository;
    @Mock private SpringDataContestRepository contestRepository;
    @Mock private SpringDataVotingResultJpaRepository votingResultRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private VotingSessionService service;

    @BeforeEach
    void setUp() {
        Supplier<UUID> ids = new FixedIds(SESSION_ID, RESULT_ID);
        service = new VotingSessionService(
                voterRecordRepository,
                electionRepository,
                ballotStyleRepository,
                votingSessionRepository,
                contestRepository,
                votingResultRepository,
                applicationEventPublisher,
                List.of(),
                ids,
                () -> NOW);
    }

    @Test
    void serviceClassIsTransactionalRequiredByDefault() {
        assertThat(VotingSessionService.class.getAnnotation(Transactional.class)).isNotNull();
    }

    @Test
    void openSessionPerformsServiceLevelDuplicateCheckBeforeSaving() {
        when(votingSessionRepository.existsByVoterRecordIdAndElectionIdAndSessionStatusNot(
                        VOTER_ID, ELECTION_ID, SessionStatus.SPOILED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.openSession(new VotingSessionService.OpenSessionCommand(
                        VOTER_ID, ELECTION_ID, BALLOT_STYLE_ID, "kiosk-01", "officer", "10.0.2.1")))
                .isInstanceOf(VotingSessionValidationException.class)
                .hasMessageContaining("duplicate");

        verify(votingSessionRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void openSessionSavesOpenedSessionAndPublishesAuditEvent() {
        VoterRecord voter = voterRecord();
        Election election = election();
        BallotStyle ballotStyle = ballotStyle(election);
        when(votingSessionRepository.existsByVoterRecordIdAndElectionIdAndSessionStatusNot(
                        VOTER_ID, ELECTION_ID, SessionStatus.SPOILED))
                .thenReturn(false);
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(voter));
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));
        when(ballotStyleRepository.findById(BALLOT_STYLE_ID)).thenReturn(Optional.of(ballotStyle));
        when(votingSessionRepository.save(any(VotingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VotingSession session = service.openSession(new VotingSessionService.OpenSessionCommand(
                VOTER_ID, ELECTION_ID, BALLOT_STYLE_ID, "kiosk-01", "officer-001", "10.0.2.1"));

        assertThat(session.getId()).isEqualTo(SESSION_ID);
        assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.OPENED);
        assertThat(session.getVotingMethod()).isEqualTo(VotingMethod.ELECTION_DAY);
        verify(votingSessionRepository).save(session);

        TransactionalAuditEvent event = captureAuditEvent();
        assertThat(event.eventType()).isEqualTo("VotingSessionOpened");
        assertThat(event.aggregateType()).isEqualTo("VotingSession");
        assertThat(event.payload())
                .containsEntry("voterId", VOTER_ID.toString())
                .containsEntry("votingMethod", VotingMethod.ELECTION_DAY.name());
    }

    @Test
    void openSessionPersistsExplicitVotingMethod() {
        VoterRecord voter = voterRecord();
        Election election = election();
        BallotStyle ballotStyle = ballotStyle(election);
        when(votingSessionRepository.existsByVoterRecordIdAndElectionIdAndSessionStatusNot(
                        VOTER_ID, ELECTION_ID, SessionStatus.SPOILED))
                .thenReturn(false);
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(voter));
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));
        when(ballotStyleRepository.findById(BALLOT_STYLE_ID)).thenReturn(Optional.of(ballotStyle));
        when(votingSessionRepository.save(any(VotingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VotingSession session = service.openSession(new VotingSessionService.OpenSessionCommand(
                VOTER_ID,
                ELECTION_ID,
                BALLOT_STYLE_ID,
                "early-kiosk-01",
                "officer-001",
                "10.0.2.1",
                VotingMethod.EARLY_VOTING));

        assertThat(session.getVotingMethod()).isEqualTo(VotingMethod.EARLY_VOTING);
        assertThat(captureAuditEvent().payload()).containsEntry("votingMethod", VotingMethod.EARLY_VOTING.name());
    }

    @Test
    void openSessionRunsConfiguredOpeningPoliciesBeforePersisting() {
        VoterRecord voter = voterRecord();
        Election election = election();
        BallotStyle ballotStyle = ballotStyle(election);
        when(votingSessionRepository.existsByVoterRecordIdAndElectionIdAndSessionStatusNot(
                        VOTER_ID, ELECTION_ID, SessionStatus.SPOILED))
                .thenReturn(false);
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(voter));
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));
        when(ballotStyleRepository.findById(BALLOT_STYLE_ID)).thenReturn(Optional.of(ballotStyle));

        VotingSessionService policyBackedService = new VotingSessionService(
                voterRecordRepository,
                electionRepository,
                ballotStyleRepository,
                votingSessionRepository,
                contestRepository,
                votingResultRepository,
                applicationEventPublisher,
                List.of(context -> {
                    assertThat(context.votingMethod()).isEqualTo(VotingMethod.EARLY_VOTING);
                    assertThat(context.homeDistrictCode()).isEqualTo("SEOUL-JONGNO");
                    assertThat(context.pollingStationDistrictCode()).isEqualTo("BUSAN-HAEUNDAE");
                    throw new VotingSessionValidationException("policy rejected session opening");
                }),
                new FixedIds(SESSION_ID, RESULT_ID),
                () -> NOW);

        assertThatThrownBy(() -> policyBackedService.openSession(new VotingSessionService.OpenSessionCommand(
                        VOTER_ID,
                        ELECTION_ID,
                        BALLOT_STYLE_ID,
                        "early-kiosk-01",
                        "officer-001",
                        "10.0.2.1",
                        VotingMethod.EARLY_VOTING,
                        "SEOUL-JONGNO",
                        "BUSAN-HAEUNDAE")))
                .isInstanceOf(VotingSessionValidationException.class)
                .hasMessageContaining("policy rejected");
        verify(votingSessionRepository, never()).save(any(VotingSession.class));
    }

    @Test
    void openSessionTranslatesDbUniqueViolationIntoDuplicateVoteRejection() {
        VoterRecord voter = voterRecord();
        Election election = election();
        BallotStyle ballotStyle = ballotStyle(election);
        when(votingSessionRepository.existsByVoterRecordIdAndElectionIdAndSessionStatusNot(
                        VOTER_ID, ELECTION_ID, SessionStatus.SPOILED))
                .thenReturn(false);
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(voter));
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));
        when(ballotStyleRepository.findById(BALLOT_STYLE_ID)).thenReturn(Optional.of(ballotStyle));
        when(votingSessionRepository.save(any(VotingSession.class)))
                .thenThrow(new DataIntegrityViolationException("uq_voting_sessions_non_spoiled_per_election"));

        assertThatThrownBy(() -> service.openSession(new VotingSessionService.OpenSessionCommand(
                        VOTER_ID, ELECTION_ID, BALLOT_STYLE_ID, "kiosk-01", "officer", "10.0.2.1")))
                .isInstanceOf(VotingSessionValidationException.class)
                .hasMessageContaining("duplicate");

        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void castBallotCreatesImmutableVotingResultCastsSessionAndPublishesVoteCastEvent() {
        VotingSession session = openedSession();
        Contest contest = contest(session.getElection());
        when(votingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(contest));
        when(votingResultRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked") Iterable<VotingResult> results = invocation.getArgument(0);
            results.forEach(VotingResult::computeHashBeforePersist);
            return results;
        });
        when(votingSessionRepository.save(any(VotingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VotingSessionService.CastBallotReceipt receipt = service.castBallot(new VotingSessionService.CastBallotCommand(
                SESSION_ID,
                List.of(new VotingSessionService.ContestSelection(CONTEST_ID, List.of(CANDIDATE_ID))),
                "voter-001",
                "10.0.2.2"));

        assertThat(session.getSessionStatus()).isEqualTo(SessionStatus.CAST);
        assertThat(receipt.sessionId()).isEqualTo(SESSION_ID);
        assertThat(receipt.resultHashes()).hasSize(1);
        assertThat(receipt.resultHashes().getFirst()).hasSize(64);
        verify(votingSessionRepository).save(session);

        TransactionalAuditEvent event = captureAuditEvent();
        assertThat(event.eventType()).isEqualTo("VoteCastEvent");
        assertThat(event.aggregateId()).isEqualTo(SESSION_ID);
        assertThat(event.payload())
                .containsEntry("electionId", ELECTION_ID.toString())
                .containsEntry("resultCount", 1);
    }

    @Test
    void castBallotRejectsNonOpenedSessionAndDoesNotCreateResult() {
        VotingSession session = openedSession();
        session.spoil(NOW);
        when(votingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.castBallot(new VotingSessionService.CastBallotCommand(
                        SESSION_ID,
                        List.of(new VotingSessionService.ContestSelection(CONTEST_ID, List.of(CANDIDATE_ID))),
                        "voter",
                        "127.0.0.1")))
                .isInstanceOf(VotingSessionValidationException.class)
                .hasMessageContaining("OPENED");

        verify(votingResultRepository, never()).saveAll(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void spoilBallotMarksSessionSpoiledAndPublishesAuditEvent() {
        VotingSession session = openedSession();
        when(votingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(votingSessionRepository.save(any(VotingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VotingSession spoiled = service.spoilBallot(SESSION_ID, "officer-003", "10.0.2.3");

        assertThat(spoiled.getSessionStatus()).isEqualTo(SessionStatus.SPOILED);
        assertThat(spoiled.getCompletedAt()).isEqualTo(NOW);
        verify(votingSessionRepository).save(session);

        TransactionalAuditEvent event = captureAuditEvent();
        assertThat(event.eventType()).isEqualTo("VotingSessionSpoiled");
    }

    @Test
    void missingSessionRejectsCastAndDoesNotEmitAuditEvent() {
        when(votingSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.castBallot(new VotingSessionService.CastBallotCommand(
                        SESSION_ID,
                        List.of(new VotingSessionService.ContestSelection(CONTEST_ID, List.of(CANDIDATE_ID))),
                        "voter",
                        "127.0.0.1")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(SESSION_ID.toString());

        verifyNoInteractions(applicationEventPublisher);
    }

    private VoterRecord voterRecord() {
        return VoterRecord.create(VOTER_ID, "EXT-1", Set.of(ELECTION_ID), RegistrationStatus.ACTIVE,
                new PiiEncryptionService("0123456789abcdef0123456789abcdef".getBytes()));
    }

    private Election election() {
        return Election.create(ELECTION_ID, "Election", ElectionType.LOCAL, "Seoul", LocalDate.of(2026, 6, 3), "KR", "ext-kr");
    }

    private BallotStyle ballotStyle(Election election) {
        Ballot ballot = Ballot.create(UUID.fromString("018f4b81-8888-7888-8888-888888888888"), election);
        return ballot.addStyle(BALLOT_STYLE_ID, "SEOUL-01", "Seoul", "ko", Set.of(AccessibilityFeature.LARGE_TEXT));
    }

    private Contest contest(Election election) {
        return Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
    }

    private VotingSession openedSession() {
        VoterRecord voter = voterRecord();
        Election election = election();
        return voter.openVotingSession(SESSION_ID, election, ballotStyle(election), "kiosk-01", NOW.minusMinutes(5));
    }

    private TransactionalAuditEvent captureAuditEvent() {
        ArgumentCaptor<TransactionalAuditEvent> captor = ArgumentCaptor.forClass(TransactionalAuditEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private static final class FixedIds implements Supplier<UUID> {
        private final UUID[] ids;
        private int index;
        private FixedIds(UUID... ids) { this.ids = ids; }
        @Override public UUID get() { return ids[index++]; }
    }
}
