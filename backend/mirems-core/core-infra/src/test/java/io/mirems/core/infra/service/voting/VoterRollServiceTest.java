package io.mirems.core.infra.service.voting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.mirems.core.bpmn.voter.VoterEligibilityDecisionService;
import io.mirems.core.bpmn.voter.VoterEligibilityRequest;
import io.mirems.core.bpmn.voter.VoterEligibilityResult;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.core.infra.persistence.voting.SpringDataVoterRecordRepository;
import io.mirems.core.infra.service.election.TransactionalAuditEvent;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class VoterRollServiceTest {
    private static final UUID VOTER_ID = UUID.fromString("018f4b80-1111-7111-8111-111111111111");
    private static final UUID ELECTION_ID = UUID.fromString("018f4b80-2222-7222-8222-222222222222");
    private static final UUID OTHER_ELECTION_ID = UUID.fromString("018f4b80-3333-7333-8333-333333333333");
    private static final String KEY = "0123456789abcdef0123456789abcdef";

    @Mock private SpringDataVoterRecordRepository voterRecordRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private VoterEligibilityDecisionService voterEligibilityDecisionService;

    private VoterRollService service;
    private PiiEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new PiiEncryptionService(KEY.getBytes());
        Supplier<UUID> ids = new FixedIds(VOTER_ID);
        service = new VoterRollService(
                voterRecordRepository,
                applicationEventPublisher,
                encryptionService,
                voterEligibilityDecisionService,
                ids);
    }

    @Test
    void serviceClassIsTransactionalRequiredByDefault() {
        Transactional transactional = VoterRollService.class.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRED);
    }

    @Test
    void registerVoterEncryptsExternalIdSavesRecordAndPublishesAuditEvent() {
        when(voterRecordRepository.save(any(VoterRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoterRecord voter = service.registerVoter(new VoterRollService.RegisterVoterCommand(
                " EXT-12345 ", Set.of(ELECTION_ID), RegistrationStatus.ACTIVE, "officer-001", "10.0.1.1"));

        assertThat(voter.getId()).isEqualTo(VOTER_ID);
        assertThat(voter.decryptExternalVoterId(encryptionService)).isEqualTo("EXT-12345");
        assertThat(voter.getEligibleElections()).containsExactly(ELECTION_ID);
        verify(voterRecordRepository).save(voter);

        TransactionalAuditEvent event = captureAuditEvent();
        assertThat(event.eventType()).isEqualTo("VoterRegistered");
        assertThat(event.aggregateId()).isEqualTo(VOTER_ID);
        assertThat(event.aggregateType()).isEqualTo("VoterRecord");
        assertThat(event.actorId()).isEqualTo("officer-001");
        assertThat(event.payload())
                .containsEntry("registrationStatus", "ACTIVE")
                .containsEntry("eligibleElectionCount", 1)
                .doesNotContainKey("externalVoterId");
    }

    @Test
    void updateEligibilityLoadsVoterReplacesEligibleElectionsAndPublishesAuditEvent() {
        VoterRecord voter = voterRecord();
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(voter));
        when(voterRecordRepository.save(any(VoterRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VoterRecord updated = service.updateEligibility(new VoterRollService.UpdateEligibilityCommand(
                VOTER_ID, Set.of(OTHER_ELECTION_ID), "officer-002", "10.0.1.2"));

        assertThat(updated.getEligibleElections()).containsExactly(OTHER_ELECTION_ID);
        verify(voterRecordRepository).save(voter);

        TransactionalAuditEvent event = captureAuditEvent();
        assertThat(event.eventType()).isEqualTo("VoterEligibilityUpdated");
        assertThat(event.payload())
                .containsEntry("voterId", VOTER_ID.toString())
                .containsEntry("eligibleElectionCount", 1);
    }

    @Test
    void checkEligibilityReturnsTrueOnlyForActiveVoterEligibleForElection() {
        VoterRecord voter = voterRecord();
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(voter));

        assertThat(service.checkEligibility(VOTER_ID, ELECTION_ID)).isTrue();
        assertThat(service.checkEligibility(VOTER_ID, OTHER_ELECTION_ID)).isFalse();
    }

    @Test
    void checkEligibilityCommandUsesVoterEligibilityDmnDecisionService() {
        VoterRecord voter = voterRecord();
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(voter));
        when(voterEligibilityDecisionService.evaluate(new VoterEligibilityRequest(
                        20, RegistrationStatus.ACTIVE, true, ElectionType.PRESIDENTIAL)))
                .thenReturn(new VoterEligibilityResult(true, "eligible"));

        VoterEligibilityResult result = service.checkEligibility(new VoterRollService.CheckVoterEligibilityCommand(
                VOTER_ID, ELECTION_ID, 20, true, ElectionType.PRESIDENTIAL));

        assertThat(result).isEqualTo(new VoterEligibilityResult(true, "eligible"));
        verify(voterEligibilityDecisionService)
                .evaluate(new VoterEligibilityRequest(20, RegistrationStatus.ACTIVE, true, ElectionType.PRESIDENTIAL));
    }

    @Test
    void checkEligibilityCommandRejectsWhenVoterIsNotAssignedToElectionBeforeCallingDmn() {
        VoterRecord voter = voterRecord();
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(voter));

        VoterEligibilityResult result = service.checkEligibility(new VoterRollService.CheckVoterEligibilityCommand(
                VOTER_ID, OTHER_ELECTION_ID, 20, true, ElectionType.PRESIDENTIAL));

        assertThat(result.eligible()).isFalse();
        assertThat(result.reason()).isEqualTo("voter is not assigned to election");
        verifyNoInteractions(voterEligibilityDecisionService);
    }

    @Test
    void checkEligibilityReturnsFalseForSuspendedVoter() {
        VoterRecord suspended = VoterRecord.create(VOTER_ID, "EXT-12345", Set.of(ELECTION_ID), RegistrationStatus.SUSPENDED, encryptionService);
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.of(suspended));

        assertThat(service.checkEligibility(VOTER_ID, ELECTION_ID)).isFalse();
    }

    @Test
    void missingVoterRejectsEligibilityUpdateAndDoesNotEmitAuditEvent() {
        when(voterRecordRepository.findById(VOTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateEligibility(new VoterRollService.UpdateEligibilityCommand(
                        VOTER_ID, Set.of(ELECTION_ID), "officer", "127.0.0.1")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(VOTER_ID.toString());

        verify(voterRecordRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
    }

    private VoterRecord voterRecord() {
        return VoterRecord.create(VOTER_ID, "EXT-12345", Set.of(ELECTION_ID), RegistrationStatus.ACTIVE, encryptionService);
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
