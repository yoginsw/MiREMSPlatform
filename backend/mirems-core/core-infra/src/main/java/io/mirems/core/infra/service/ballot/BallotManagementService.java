package io.mirems.core.infra.service.ballot;

import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.election.Election;
import io.mirems.core.infra.persistence.ballot.SpringDataBallotRepository;
import io.mirems.core.infra.persistence.ballot.SpringDataBallotStyleRepository;
import io.mirems.core.infra.persistence.contest.SpringDataContestRepository;
import io.mirems.core.infra.persistence.election.SpringDataElectionRepository;
import io.mirems.core.infra.service.election.TransactionalAuditEvent;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(SpringDataElectionRepository.class)
@Transactional
public class BallotManagementService {
    private final SpringDataElectionRepository electionRepository;
    private final SpringDataContestRepository contestRepository;
    private final SpringDataBallotRepository ballotRepository;
    private final SpringDataBallotStyleRepository ballotStyleRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Supplier<UUID> idGenerator;

    public BallotManagementService(
            SpringDataElectionRepository electionRepository,
            SpringDataContestRepository contestRepository,
            SpringDataBallotRepository ballotRepository,
            SpringDataBallotStyleRepository ballotStyleRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this(electionRepository, contestRepository, ballotRepository, ballotStyleRepository,
                applicationEventPublisher, UUID::randomUUID);
    }

    BallotManagementService(
            SpringDataElectionRepository electionRepository,
            SpringDataContestRepository contestRepository,
            SpringDataBallotRepository ballotRepository,
            SpringDataBallotStyleRepository ballotStyleRepository,
            ApplicationEventPublisher applicationEventPublisher,
            Supplier<UUID> idGenerator) {
        this.electionRepository = electionRepository;
        this.contestRepository = contestRepository;
        this.ballotRepository = ballotRepository;
        this.ballotStyleRepository = ballotStyleRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.idGenerator = idGenerator;
    }

    public Ballot createBallot(CreateBallotCommand command) {
        Election election = electionRepository.findById(command.electionId())
                .orElseThrow(() -> new EntityNotFoundException("Election not found: " + command.electionId()));
        Ballot ballot = Ballot.create(idGenerator.get(), election);
        addContests(ballot, command.contestIds());
        Ballot saved = ballotRepository.save(ballot);
        publishAudit("BallotCreated", saved.getId(), "Ballot", Map.of(
                "electionId", command.electionId().toString(),
                "ballotVersion", saved.getBallotVersion()), command.actorId(), command.sourceIp());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Ballot> listBallots(UUID electionId) {
        return ballotRepository.findByElectionId(electionId);
    }

    public Ballot createBallotVersion(CreateBallotVersionCommand command) {
        Ballot ballot = ballotRepository.findById(command.ballotId())
                .filter(candidate -> candidate.getElection().getId().equals(command.electionId()))
                .orElseThrow(() -> new EntityNotFoundException("Ballot not found: " + command.ballotId()));
        addContests(ballot, command.contestIds());
        Ballot saved = ballotRepository.save(ballot);
        publishAudit("BallotVersionCreated", saved.getId(), "Ballot", Map.of(
                        "electionId", command.electionId().toString(),
                        "ballotVersion", saved.getBallotVersion(),
                        "changeReason", command.changeReason()),
                command.actorId(), command.sourceIp());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Ballot> previewBallot(UUID electionId, UUID ballotId) {
        return ballotRepository.findById(ballotId)
                .filter(ballot -> ballot.getElection().getId().equals(electionId));
    }

    public BallotStyle createBallotStyle(CreateBallotStyleCommand command) {
        Ballot ballot = ballotRepository.findById(command.ballotId())
                .filter(candidate -> candidate.getElection().getId().equals(command.electionId()))
                .orElseThrow(() -> new EntityNotFoundException("Ballot not found: " + command.ballotId()));
        BallotStyle style = ballot.addStyle(
                idGenerator.get(),
                command.styleCode(),
                command.district(),
                command.language(),
                command.accessibilityFeatures());
        BallotStyle saved = ballotStyleRepository.save(style);
        publishAudit("BallotStyleCreated", saved.getId(), "BallotStyle", Map.of(
                        "ballotId", command.ballotId().toString(),
                        "styleCode", saved.getStyleCode()),
                command.actorId(), command.sourceIp());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BallotStyle> listBallotStyles(UUID electionId) {
        return ballotRepository.findByElectionId(electionId).stream()
                .flatMap(ballot -> ballot.getBallotStyles().stream())
                .toList();
    }

    public BallotStyle updateBallotStyle(UpdateBallotStyleCommand command) {
        BallotStyle style = ballotStyleRepository.findById(command.ballotStyleId())
                .filter(candidate -> candidate.getBallot().getElection().getId().equals(command.electionId()))
                .orElseThrow(() -> new EntityNotFoundException("Ballot style not found: " + command.ballotStyleId()));
        style.updateDetails(command.styleCode(), command.district(), command.language(), command.accessibilityFeatures());
        BallotStyle saved = ballotStyleRepository.save(style);
        publishAudit("BallotStyleUpdated", saved.getId(), "BallotStyle", Map.of(
                        "styleCode", saved.getStyleCode(),
                        "language", saved.getLanguage()),
                command.actorId(), command.sourceIp());
        return saved;
    }

    public void deleteBallotStyle(UUID electionId, UUID ballotStyleId, String actorId, String sourceIp) {
        BallotStyle style = ballotStyleRepository.findById(ballotStyleId)
                .filter(candidate -> candidate.getBallot().getElection().getId().equals(electionId))
                .orElseThrow(() -> new EntityNotFoundException("Ballot style not found: " + ballotStyleId));
        ballotStyleRepository.delete(style);
        publishAudit("BallotStyleDeleted", ballotStyleId, "BallotStyle", Map.of(
                        "ballotId", style.getBallot().getId().toString()),
                actorId, sourceIp);
    }

    private void addContests(Ballot ballot, List<UUID> contestIds) {
        int order = ballot.getBallotContests().size() + 1;
        for (UUID contestId : contestIds) {
            Contest contest = contestRepository.findById(contestId)
                    .orElseThrow(() -> new EntityNotFoundException("Contest not found: " + contestId));
            ballot.addContest(contest, order++, contest.getName());
        }
    }

    private void publishAudit(
            String eventType,
            UUID aggregateId,
            String aggregateType,
            Map<String, Object> payload,
            String actorId,
            String sourceIp) {
        applicationEventPublisher.publishEvent(
                new TransactionalAuditEvent(eventType, aggregateId, aggregateType, payload, actorId, sourceIp));
    }

    public record CreateBallotCommand(UUID electionId, List<UUID> contestIds, String actorId, String sourceIp) {}

    public record CreateBallotVersionCommand(
            UUID electionId, UUID ballotId, String changeReason, List<UUID> contestIds, String actorId, String sourceIp) {}

    public record CreateBallotStyleCommand(
            UUID electionId,
            UUID ballotId,
            String styleCode,
            String district,
            String language,
            Set<AccessibilityFeature> accessibilityFeatures,
            String actorId,
            String sourceIp) {}

    public record UpdateBallotStyleCommand(
            UUID electionId,
            UUID ballotStyleId,
            UUID ballotId,
            String styleCode,
            String district,
            String language,
            Set<AccessibilityFeature> accessibilityFeatures,
            String actorId,
            String sourceIp) {}
}
