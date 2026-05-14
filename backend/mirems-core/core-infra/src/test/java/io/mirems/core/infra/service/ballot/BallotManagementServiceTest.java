package io.mirems.core.infra.service.ballot;

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
import io.mirems.core.infra.persistence.ballot.SpringDataBallotRepository;
import io.mirems.core.infra.persistence.ballot.SpringDataBallotStyleRepository;
import io.mirems.core.infra.persistence.contest.SpringDataContestRepository;
import io.mirems.core.infra.persistence.election.SpringDataElectionRepository;
import io.mirems.core.infra.service.election.TransactionalAuditEvent;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
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
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class BallotManagementServiceTest {
    private static final UUID ELECTION_ID = UUID.fromString("10000000-0000-0000-0000-000000000132");
    private static final UUID BALLOT_ID = UUID.fromString("20000000-0000-0000-0000-000000000132");
    private static final UUID STYLE_ID = UUID.fromString("30000000-0000-0000-0000-000000000132");
    private static final UUID CONTEST_ID = UUID.fromString("40000000-0000-0000-0000-000000000132");
    private static final UUID NEXT_ID = UUID.fromString("50000000-0000-0000-0000-000000000132");

    @Mock
    private SpringDataElectionRepository electionRepository;
    @Mock
    private SpringDataContestRepository contestRepository;
    @Mock
    private SpringDataBallotRepository ballotRepository;
    @Mock
    private SpringDataBallotStyleRepository ballotStyleRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private BallotManagementService service;

    @BeforeEach
    void setUp() {
        Supplier<UUID> idGenerator = new FixedIds(BALLOT_ID, STYLE_ID, NEXT_ID);
        service = new BallotManagementService(electionRepository, contestRepository, ballotRepository,
                ballotStyleRepository, applicationEventPublisher, idGenerator);
    }

    @Test
    void serviceClassIsTransactional() {
        assertThat(BallotManagementService.class.getAnnotation(Transactional.class)).isNotNull();
    }

    @Test
    void createBallotLoadsElectionAndContestsSavesBallotAndPublishesAudit() {
        Election election = election();
        Contest contest = contest(election);
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(contest));
        when(ballotRepository.save(any(Ballot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ballot ballot = service.createBallot(new BallotManagementService.CreateBallotCommand(
                ELECTION_ID, List.of(CONTEST_ID), "admin-032", "10.0.0.32"));

        assertThat(ballot.getId()).isEqualTo(BALLOT_ID);
        assertThat(ballot.getElection()).isSameAs(election);
        assertThat(ballot.getBallotContests()).hasSize(1);
        assertThat(ballot.getBallotVersion()).isEqualTo(2);
        verify(ballotRepository).save(ballot);
        TransactionalAuditEvent event = capturePublishedEvent();
        assertThat(event.eventType()).isEqualTo("BallotCreated");
        assertThat(event.aggregateId()).isEqualTo(BALLOT_ID);
        assertThat(event.actorId()).isEqualTo("admin-032");
    }

    @Test
    void createVersionAndPreviewUseExistingBallot() {
        Election election = election();
        Contest contest = contest(election);
        Ballot ballot = Ballot.create(BALLOT_ID, election);
        when(ballotRepository.findById(BALLOT_ID)).thenReturn(Optional.of(ballot));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(contest));
        when(ballotRepository.save(any(Ballot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ballot versioned = service.createBallotVersion(new BallotManagementService.CreateBallotVersionCommand(
                ELECTION_ID, BALLOT_ID, "Add presidential contest", List.of(CONTEST_ID), "admin", "127.0.0.1"));

        assertThat(versioned.getBallotVersion()).isEqualTo(2);
        assertThat(service.previewBallot(ELECTION_ID, BALLOT_ID)).contains(ballot);
        verify(ballotRepository).save(ballot);
        assertThat(capturePublishedEvent().eventType()).isEqualTo("BallotVersionCreated");
    }

    @Test
    void ballotStyleCrudUsesRepositoriesAndAuditEvents() {
        Election election = election();
        Ballot ballot = Ballot.create(BALLOT_ID, election);
        when(ballotRepository.findById(BALLOT_ID)).thenReturn(Optional.of(ballot));
        when(ballotStyleRepository.save(any(BallotStyle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BallotStyle style = service.createBallotStyle(new BallotManagementService.CreateBallotStyleCommand(
                ELECTION_ID, BALLOT_ID, "US-FED-EN", "US-FED", "en",
                Set.of(AccessibilityFeature.AUDIO), "admin", "127.0.0.1"));

        assertThat(style.getId()).isEqualTo(BALLOT_ID); // first fixed id for this independent service instance
        assertThat(style.getStyleCode()).isEqualTo("US-FED-EN");
        verify(ballotStyleRepository).save(style);
        assertThat(capturePublishedEvent().eventType()).isEqualTo("BallotStyleCreated");
    }

    @Test
    void updateDeleteAndListBallotStylesUseRepositories() {
        Election election = election();
        Ballot ballot = Ballot.create(BALLOT_ID, election);
        BallotStyle style = ballot.addStyle(STYLE_ID, "US-FED-EN", "US-FED", "en", Set.of(AccessibilityFeature.AUDIO));
        when(ballotRepository.findByElectionId(ELECTION_ID)).thenReturn(List.of(ballot));
        when(ballotStyleRepository.findById(STYLE_ID)).thenReturn(Optional.of(style));
        when(ballotStyleRepository.save(any(BallotStyle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.listBallotStyles(ELECTION_ID)).containsExactly(style);
        BallotStyle updated = service.updateBallotStyle(new BallotManagementService.UpdateBallotStyleCommand(
                ELECTION_ID, STYLE_ID, BALLOT_ID, "US-FED-ES", "US-FED", "es",
                Set.of(AccessibilityFeature.LARGE_TEXT), "admin", "127.0.0.1"));
        service.deleteBallotStyle(ELECTION_ID, STYLE_ID, "admin", "127.0.0.1");

        assertThat(updated.getStyleCode()).isEqualTo("US-FED-ES");
        assertThat(updated.getLanguage()).isEqualTo("es");
        verify(ballotStyleRepository).delete(style);
    }

    @Test
    void missingElectionRejectsBallotCreationAndDoesNotPublishAudit() {
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBallot(new BallotManagementService.CreateBallotCommand(
                        ELECTION_ID, List.of(CONTEST_ID), "admin", "127.0.0.1")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(ELECTION_ID.toString());

        verify(ballotRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    private TransactionalAuditEvent capturePublishedEvent() {
        ArgumentCaptor<TransactionalAuditEvent> captor = ArgumentCaptor.forClass(TransactionalAuditEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private Election election() {
        Election election = Election.create(ELECTION_ID, "2028 General Election", ElectionType.PRESIDENTIAL,
                "US-FED", LocalDate.of(2028, 11, 7), "US", "ext-us");
        election.pullDomainEvents();
        return election;
    }

    private Contest contest(Election election) {
        return Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "President", 1, 1);
    }

    private static final class FixedIds implements Supplier<UUID> {
        private final UUID[] ids;
        private int index;

        private FixedIds(UUID... ids) {
            this.ids = ids;
        }

        @Override
        public UUID get() {
            return ids[index++];
        }
    }
}
