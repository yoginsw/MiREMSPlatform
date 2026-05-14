package io.mirems.core.infra.service.voting;

import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.core.infra.persistence.voting.SpringDataVoterRecordRepository;
import io.mirems.core.infra.service.election.TransactionalAuditEvent;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Direct service for voter roll registration and eligibility checks. */
@Service
@ConditionalOnBean(SpringDataVoterRecordRepository.class)
@Transactional
public class VoterRollService {
    private final SpringDataVoterRecordRepository voterRecordRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PiiEncryptionService encryptionService;
    private final Supplier<UUID> idGenerator;

    public VoterRollService(
            SpringDataVoterRecordRepository voterRecordRepository,
            ApplicationEventPublisher applicationEventPublisher,
            PiiEncryptionService encryptionService) {
        this(voterRecordRepository, applicationEventPublisher, encryptionService, UUID::randomUUID);
    }

    VoterRollService(
            SpringDataVoterRecordRepository voterRecordRepository,
            ApplicationEventPublisher applicationEventPublisher,
            PiiEncryptionService encryptionService,
            Supplier<UUID> idGenerator) {
        this.voterRecordRepository = voterRecordRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.encryptionService = encryptionService;
        this.idGenerator = idGenerator;
    }

    public VoterRecord registerVoter(RegisterVoterCommand command) {
        VoterRecord voterRecord = VoterRecord.create(
                idGenerator.get(),
                command.externalVoterId(),
                command.eligibleElections(),
                command.registrationStatus(),
                encryptionService);
        VoterRecord saved = voterRecordRepository.save(voterRecord);
        publishAuditEvent(
                "VoterRegistered",
                saved.getId(),
                Map.of(
                        "voterId", saved.getId().toString(),
                        "registrationStatus", saved.getRegistrationStatus().name(),
                        "eligibleElectionCount", saved.getEligibleElections().size()),
                command.actorId(),
                command.sourceIp());
        return saved;
    }

    public VoterRecord updateEligibility(UpdateEligibilityCommand command) {
        VoterRecord voterRecord = findVoter(command.voterId());
        voterRecord.updateEligibility(command.eligibleElections());
        VoterRecord saved = voterRecordRepository.save(voterRecord);
        publishAuditEvent(
                "VoterEligibilityUpdated",
                saved.getId(),
                Map.of(
                        "voterId", saved.getId().toString(),
                        "eligibleElectionCount", saved.getEligibleElections().size()),
                command.actorId(),
                command.sourceIp());
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean checkEligibility(UUID voterId, UUID electionId) {
        return findVoter(voterId).isEligibleFor(electionId);
    }

    private VoterRecord findVoter(UUID voterId) {
        return voterRecordRepository
                .findById(voterId)
                .orElseThrow(() -> new EntityNotFoundException("VoterRecord not found: " + voterId));
    }

    private void publishAuditEvent(
            String eventType, UUID aggregateId, Map<String, Object> payload, String actorId, String sourceIp) {
        applicationEventPublisher.publishEvent(
                new TransactionalAuditEvent(eventType, aggregateId, "VoterRecord", payload, actorId, sourceIp));
    }

    public record RegisterVoterCommand(
            String externalVoterId,
            Set<UUID> eligibleElections,
            RegistrationStatus registrationStatus,
            String actorId,
            String sourceIp) {}

    public record UpdateEligibilityCommand(UUID voterId, Set<UUID> eligibleElections, String actorId, String sourceIp) {}
}
