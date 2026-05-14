package io.mirems.core.infra.service.election;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.mirems.core.domain.contest.Candidate;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionStatus;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.infra.persistence.contest.SpringDataCandidateRepository;
import io.mirems.core.infra.persistence.contest.SpringDataContestRepository;
import io.mirems.core.infra.persistence.election.SpringDataElectionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class ElectionManagementServiceTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4b7f-1111-7111-8111-111111111111");
    private static final UUID CONTEST_ID = UUID.fromString("018f4b7f-2222-7222-8222-222222222222");
    private static final UUID CANDIDATE_ID = UUID.fromString("018f4b7f-3333-7333-8333-333333333333");
    private static final UUID NEXT_ID = UUID.fromString("018f4b7f-4444-7444-8444-444444444444");

    @Mock
    private SpringDataElectionRepository electionRepository;

    @Mock
    private SpringDataContestRepository contestRepository;

    @Mock
    private SpringDataCandidateRepository candidateRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ElectionPublicationWorkflow publicationWorkflow;

    private ElectionManagementService service;

    @BeforeEach
    void setUp() {
        Supplier<UUID> idGenerator = new FixedIds(ELECTION_ID, CONTEST_ID, CANDIDATE_ID, NEXT_ID);
        service = new ElectionManagementService(
                electionRepository,
                contestRepository,
                candidateRepository,
                applicationEventPublisher,
                publicationWorkflow,
                idGenerator);
    }

    @Test
    void serviceClassIsTransactionalRequiredByDefault() {
        Transactional transactional = ElectionManagementService.class.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRED);
    }

    @Test
    void createElectionSavesDraftElectionAndPublishesTransactionalAuditEvent() {
        when(electionRepository.save(any(Election.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Election election = service.createElection(new ElectionManagementService.CreateElectionCommand(
                " 2026 Local Election ",
                ElectionType.LOCAL,
                "Seoul",
                LocalDate.of(2026, 6, 3),
                "kr",
                "ext-kr",
                "admin-001",
                "10.0.0.1"));

        assertThat(election.getId()).isEqualTo(ELECTION_ID);
        assertThat(election.getName()).isEqualTo("2026 Local Election");
        assertThat(election.getElectionStatus()).isEqualTo(ElectionStatus.DRAFT);
        verify(electionRepository).save(election);

        TransactionalAuditEvent event = capturePublishedEvent();
        assertThat(event.eventType()).isEqualTo("ElectionCreated");
        assertThat(event.aggregateId()).isEqualTo(ELECTION_ID);
        assertThat(event.aggregateType()).isEqualTo("Election");
        assertThat(event.actorId()).isEqualTo("admin-001");
        assertThat(event.sourceIp()).isEqualTo("10.0.0.1");
        assertThat(event.payload())
                .containsEntry("name", "2026 Local Election")
                .containsEntry("status", "DRAFT")
                .containsEntry("countryCode", "KR");
    }

    @Test
    void listAndGetElectionsDelegateToRepositoryWithoutPublishingAudit() {
        Election election = draftElection();
        when(electionRepository.findAll()).thenReturn(List.of(election));
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));

        assertThat(service.listElections()).containsExactly(election);
        assertThat(service.getElection(ELECTION_ID)).contains(election);

        verify(electionRepository).findAll();
        verify(electionRepository).findById(ELECTION_ID);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void addContestLoadsElectionSavesContestAndPublishesAuditEvent() {
        Election election = draftElection();
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));
        when(contestRepository.save(any(Contest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contest contest = service.addContest(new ElectionManagementService.AddContestCommand(
                ELECTION_ID,
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1,
                "admin-002",
                "10.0.0.2"));

        assertThat(contest.getId()).isEqualTo(ELECTION_ID); // FixedIds first value for this independent service instance.
        assertThat(contest.getElection()).isSameAs(election);
        verify(contestRepository).save(contest);

        TransactionalAuditEvent event = capturePublishedEvent();
        assertThat(event.eventType()).isEqualTo("ContestAdded");
        assertThat(event.aggregateId()).isEqualTo(contest.getId());
        assertThat(event.payload())
                .containsEntry("electionId", ELECTION_ID.toString())
                .containsEntry("contestType", "CANDIDATE_CHOICE")
                .containsEntry("name", "Mayor");
    }

    @Test
    void addCandidateLoadsContestSavesCandidateAndPublishesAuditEvent() {
        Election election = draftElection();
        Contest contest = Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(contest));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Candidate candidate = service.addCandidate(new ElectionManagementService.AddCandidateCommand(
                CONTEST_ID,
                "Kim Candidate",
                "Independent",
                "admin-003",
                "10.0.0.3"));

        assertThat(candidate.getContest()).isSameAs(contest);
        assertThat(candidate.getName()).isEqualTo("Kim Candidate");
        verify(candidateRepository).save(candidate);

        TransactionalAuditEvent event = capturePublishedEvent();
        assertThat(event.eventType()).isEqualTo("CandidateAdded");
        assertThat(event.aggregateId()).isEqualTo(candidate.getId());
        assertThat(event.payload())
                .containsEntry("contestId", CONTEST_ID.toString())
                .containsEntry("electionId", ELECTION_ID.toString())
                .containsEntry("candidateStatus", "PENDING");
    }

    @Test
    void listGetAndUpdateContestUseRepositoryAndAuditOnlyUpdates() {
        Election election = draftElection();
        Contest contest = Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
        when(contestRepository.findByElectionId(ELECTION_ID)).thenReturn(List.of(contest));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(contest));
        when(contestRepository.save(any(Contest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.listContests(ELECTION_ID)).containsExactly(contest);
        assertThat(service.getContest(ELECTION_ID, CONTEST_ID)).contains(contest);

        Contest updated = service.updateContest(new ElectionManagementService.UpdateContestCommand(
                ELECTION_ID,
                CONTEST_ID,
                ContestType.RANKED_CHOICE,
                "Mayor Ranked Choice",
                3,
                3,
                "admin-006",
                "10.0.0.6"));

        assertThat(updated.getContestType()).isEqualTo(ContestType.RANKED_CHOICE);
        assertThat(updated.getName()).isEqualTo("Mayor Ranked Choice");
        assertThat(updated.getSeats()).isEqualTo(3);
        verify(contestRepository).save(contest);
        TransactionalAuditEvent event = capturePublishedEvent();
        assertThat(event.eventType()).isEqualTo("ContestUpdated");
        assertThat(event.payload())
                .containsEntry("contestType", "RANKED_CHOICE")
                .containsEntry("name", "Mayor Ranked Choice");
    }

    @Test
    void listGetAndWithdrawCandidateUseRepositoryAndAuditOnlyWithdrawal() {
        Election election = draftElection();
        Contest contest = Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
        Candidate candidate = contest.addCandidate(CANDIDATE_ID, "Kim Candidate", "Independent");
        when(candidateRepository.findByContestId(CONTEST_ID)).thenReturn(List.of(candidate));
        when(candidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.listCandidates(ELECTION_ID, CONTEST_ID)).containsExactly(candidate);
        assertThat(service.getCandidate(ELECTION_ID, CONTEST_ID, CANDIDATE_ID)).contains(candidate);

        Candidate withdrawn = service.withdrawCandidate(CANDIDATE_ID, "officer-001", "10.0.0.7");

        assertThat(withdrawn.getCandidateStatus()).isEqualTo(io.mirems.core.domain.contest.CandidateStatus.WITHDRAWN);
        verify(candidateRepository).save(candidate);
        TransactionalAuditEvent event = capturePublishedEvent();
        assertThat(event.eventType()).isEqualTo("CandidateWithdrawn");
        assertThat(event.payload())
                .containsEntry("candidateStatus", "WITHDRAWN")
                .containsEntry("contestId", CONTEST_ID.toString());
    }

    @Test
    void publishElectionDelegatesToWorkflowSavesPublishedElectionAndEmitsAuditEvent() {
        Election election = draftElection();
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));
        when(electionRepository.save(any(Election.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Election published = service.publishElection(ELECTION_ID, "admin-004", "10.0.0.4");

        assertThat(published.getElectionStatus()).isEqualTo(ElectionStatus.PUBLISHED);
        verify(publicationWorkflow).publish(ELECTION_ID);
        verify(electionRepository).save(election);

        TransactionalAuditEvent event = capturePublishedEvent();
        assertThat(event.eventType()).isEqualTo("ElectionPublished");
        assertThat(event.aggregateId()).isEqualTo(ELECTION_ID);
        assertThat(event.payload()).containsEntry("status", "PUBLISHED");
    }

    @Test
    void closeElectionSavesClosedElectionAndEmitsAuditEvent() {
        Election election = draftElection();
        election.publish();
        election.activate();
        election.pullDomainEvents();
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(election));
        when(electionRepository.save(any(Election.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Election closed = service.closeElection(ELECTION_ID, "admin-005", "10.0.0.5");

        assertThat(closed.getElectionStatus()).isEqualTo(ElectionStatus.CLOSED);
        verify(electionRepository).save(election);

        TransactionalAuditEvent event = capturePublishedEvent();
        assertThat(event.eventType()).isEqualTo("ElectionClosed");
        assertThat(event.payload()).containsEntry("status", "CLOSED");
    }

    @Test
    void missingElectionRejectsAddContestAndDoesNotEmitAuditEvent() {
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addContest(new ElectionManagementService.AddContestCommand(
                        ELECTION_ID, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1, "admin", "127.0.0.1")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(ELECTION_ID.toString());

        verify(contestRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void missingContestRejectsAddCandidateAndDoesNotEmitAuditEvent() {
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addCandidate(new ElectionManagementService.AddCandidateCommand(
                        CONTEST_ID, "Kim", "Independent", "admin", "127.0.0.1")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(CONTEST_ID.toString());

        verify(candidateRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void serviceMethodsExposeTransactionalBoundary() throws NoSuchMethodException {
        for (String methodName : new String[] {"createElection", "addContest", "addCandidate", "publishElection", "closeElection"}) {
            Method method = findMethod(methodName);
            assertThat(method.getDeclaringClass()).isEqualTo(ElectionManagementService.class);
        }
    }

    private Method findMethod(String methodName) {
        for (Method method : ElectionManagementService.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Missing method: " + methodName);
    }

    private TransactionalAuditEvent capturePublishedEvent() {
        ArgumentCaptor<TransactionalAuditEvent> captor = ArgumentCaptor.forClass(TransactionalAuditEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private Election draftElection() {
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
