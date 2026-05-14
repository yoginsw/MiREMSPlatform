package io.mirems.core.infra.service.election;

import io.mirems.core.domain.contest.Candidate;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.infra.persistence.contest.SpringDataCandidateRepository;
import io.mirems.core.infra.persistence.contest.SpringDataContestRepository;
import io.mirems.core.infra.persistence.election.SpringDataElectionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Direct application service for managing elections before BPMN-heavy workflows are introduced. */
@Service
@ConditionalOnBean(SpringDataElectionRepository.class)
@Transactional
public class ElectionManagementService {
    private final SpringDataElectionRepository electionRepository;
    private final SpringDataContestRepository contestRepository;
    private final SpringDataCandidateRepository candidateRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ElectionPublicationWorkflow publicationWorkflow;
    private final Supplier<UUID> idGenerator;

    public ElectionManagementService(
            SpringDataElectionRepository electionRepository,
            SpringDataContestRepository contestRepository,
            SpringDataCandidateRepository candidateRepository,
            ApplicationEventPublisher applicationEventPublisher,
            ElectionPublicationWorkflow publicationWorkflow) {
        this(electionRepository, contestRepository, candidateRepository, applicationEventPublisher, publicationWorkflow, UUID::randomUUID);
    }

    ElectionManagementService(
            SpringDataElectionRepository electionRepository,
            SpringDataContestRepository contestRepository,
            SpringDataCandidateRepository candidateRepository,
            ApplicationEventPublisher applicationEventPublisher,
            ElectionPublicationWorkflow publicationWorkflow,
            Supplier<UUID> idGenerator) {
        this.electionRepository = electionRepository;
        this.contestRepository = contestRepository;
        this.candidateRepository = candidateRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.publicationWorkflow = publicationWorkflow;
        this.idGenerator = idGenerator;
    }

    public Election createElection(CreateElectionCommand command) {
        Election election = Election.create(
                idGenerator.get(),
                command.name(),
                command.electionType(),
                command.jurisdiction(),
                command.scheduledDate(),
                command.countryCode(),
                command.extensionPackId());
        Election saved = electionRepository.save(election);
        publishAuditEvent(
                "ElectionCreated",
                saved.getId(),
                Map.of(
                        "name", saved.getName(),
                        "electionType", saved.getElectionType().name(),
                        "jurisdiction", saved.getJurisdiction(),
                        "scheduledDate", saved.getScheduledDate().toString(),
                        "countryCode", saved.getCountryCode(),
                        "extensionPackId", saved.getExtensionPackId(),
                        "status", saved.getElectionStatus().name()),
                command.actorId(),
                command.sourceIp());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Election> listElections() {
        return electionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Election> getElection(UUID electionId) {
        return electionRepository.findById(electionId);
    }

    public Contest addContest(AddContestCommand command) {
        Election election = findElection(command.electionId());
        Contest contest = Contest.create(
                idGenerator.get(),
                election,
                command.contestType(),
                command.name(),
                command.seats(),
                command.voteLimit());
        Contest saved = contestRepository.save(contest);
        publishAuditEvent(
                "ContestAdded",
                saved.getId(),
                Map.of(
                        "electionId", election.getId().toString(),
                        "contestType", saved.getContestType().name(),
                        "name", saved.getName(),
                        "seats", saved.getSeats(),
                        "voteLimit", saved.getVoteLimit()),
                command.actorId(),
                command.sourceIp());
        return saved;
    }

    public Candidate addCandidate(AddCandidateCommand command) {
        Contest contest = contestRepository
                .findById(command.contestId())
                .orElseThrow(() -> new EntityNotFoundException("Contest not found: " + command.contestId()));
        Candidate candidate = contest.addCandidate(idGenerator.get(), command.name(), command.partyAffiliation());
        Candidate saved = candidateRepository.save(candidate);
        publishAuditEvent(
                "CandidateAdded",
                saved.getId(),
                Map.of(
                        "contestId", contest.getId().toString(),
                        "electionId", contest.getElection().getId().toString(),
                        "name", saved.getName(),
                        "partyAffiliation", saved.getPartyAffiliation(),
                        "candidateStatus", saved.getCandidateStatus().name()),
                command.actorId(),
                command.sourceIp());
        return saved;
    }

    public Election publishElection(UUID electionId, String actorId, String sourceIp) {
        Election election = findElection(electionId);
        publicationWorkflow.publish(electionId);
        election.publish();
        Election saved = electionRepository.save(election);
        publishAuditEvent(
                "ElectionPublished",
                saved.getId(),
                Map.of("status", saved.getElectionStatus().name()),
                actorId,
                sourceIp);
        return saved;
    }

    public Election closeElection(UUID electionId, String actorId, String sourceIp) {
        Election election = findElection(electionId);
        election.close();
        Election saved = electionRepository.save(election);
        publishAuditEvent(
                "ElectionClosed",
                saved.getId(),
                Map.of("status", saved.getElectionStatus().name()),
                actorId,
                sourceIp);
        return saved;
    }

    private Election findElection(UUID electionId) {
        return electionRepository
                .findById(electionId)
                .orElseThrow(() -> new EntityNotFoundException("Election not found: " + electionId));
    }

    private void publishAuditEvent(
            String eventType, UUID aggregateId, Map<String, Object> payload, String actorId, String sourceIp) {
        applicationEventPublisher.publishEvent(
                new TransactionalAuditEvent(eventType, aggregateId, "Election", payload, actorId, sourceIp));
    }

    public record CreateElectionCommand(
            String name,
            ElectionType electionType,
            String jurisdiction,
            LocalDate scheduledDate,
            String countryCode,
            String extensionPackId,
            String actorId,
            String sourceIp) {}

    public record AddContestCommand(
            UUID electionId,
            ContestType contestType,
            String name,
            int seats,
            int voteLimit,
            String actorId,
            String sourceIp) {}

    public record AddCandidateCommand(
            UUID contestId, String name, String partyAffiliation, String actorId, String sourceIp) {}
}
