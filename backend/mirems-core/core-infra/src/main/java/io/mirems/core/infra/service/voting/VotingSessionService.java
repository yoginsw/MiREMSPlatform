package io.mirems.core.infra.service.voting;

import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.result.VotingResult;
import io.mirems.core.domain.voting.SessionStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.VotingMethod;
import io.mirems.core.domain.voting.VotingSession;
import io.mirems.core.domain.voting.VotingSessionOpeningContext;
import io.mirems.core.domain.voting.VotingSessionOpeningPolicy;
import io.mirems.core.domain.voting.VotingSessionValidationException;
import io.mirems.core.infra.persistence.ballot.SpringDataBallotStyleRepository;
import io.mirems.core.infra.persistence.contest.SpringDataContestRepository;
import io.mirems.core.infra.persistence.election.SpringDataElectionRepository;
import io.mirems.core.infra.persistence.result.SpringDataVotingResultJpaRepository;
import io.mirems.core.infra.persistence.voting.SpringDataVoterRecordRepository;
import io.mirems.core.infra.persistence.voting.SpringDataVotingSessionRepository;
import io.mirems.core.infra.service.election.TransactionalAuditEvent;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Direct service for opening, casting, and spoiling voting sessions. */
@Service
@ConditionalOnBean(SpringDataVotingSessionRepository.class)
@Transactional
public class VotingSessionService {
    private final SpringDataVoterRecordRepository voterRecordRepository;
    private final SpringDataElectionRepository electionRepository;
    private final SpringDataBallotStyleRepository ballotStyleRepository;
    private final SpringDataVotingSessionRepository votingSessionRepository;
    private final SpringDataContestRepository contestRepository;
    private final SpringDataVotingResultJpaRepository votingResultRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final List<VotingSessionOpeningPolicy> openingPolicies;
    private final Supplier<UUID> idGenerator;
    private final Supplier<OffsetDateTime> clock;

    public VotingSessionService(
            SpringDataVoterRecordRepository voterRecordRepository,
            SpringDataElectionRepository electionRepository,
            SpringDataBallotStyleRepository ballotStyleRepository,
            SpringDataVotingSessionRepository votingSessionRepository,
            SpringDataContestRepository contestRepository,
            SpringDataVotingResultJpaRepository votingResultRepository,
            ApplicationEventPublisher applicationEventPublisher,
            List<VotingSessionOpeningPolicy> openingPolicies) {
        this(
                voterRecordRepository,
                electionRepository,
                ballotStyleRepository,
                votingSessionRepository,
                contestRepository,
                votingResultRepository,
                applicationEventPublisher,
                openingPolicies,
                UUID::randomUUID,
                OffsetDateTime::now);
    }

    VotingSessionService(
            SpringDataVoterRecordRepository voterRecordRepository,
            SpringDataElectionRepository electionRepository,
            SpringDataBallotStyleRepository ballotStyleRepository,
            SpringDataVotingSessionRepository votingSessionRepository,
            SpringDataContestRepository contestRepository,
            SpringDataVotingResultJpaRepository votingResultRepository,
            ApplicationEventPublisher applicationEventPublisher,
            List<VotingSessionOpeningPolicy> openingPolicies,
            Supplier<UUID> idGenerator,
            Supplier<OffsetDateTime> clock) {
        this.voterRecordRepository = voterRecordRepository;
        this.electionRepository = electionRepository;
        this.ballotStyleRepository = ballotStyleRepository;
        this.votingSessionRepository = votingSessionRepository;
        this.contestRepository = contestRepository;
        this.votingResultRepository = votingResultRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.openingPolicies = List.copyOf(openingPolicies);
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public VotingSession openSession(OpenSessionCommand command) {
        ensureNoExistingNonSpoiledSession(command.voterId(), command.electionId());
        VoterRecord voterRecord = voterRecordRepository
                .findById(command.voterId())
                .orElseThrow(() -> new EntityNotFoundException("VoterRecord not found: " + command.voterId()));
        Election election = electionRepository
                .findById(command.electionId())
                .orElseThrow(() -> new EntityNotFoundException("Election not found: " + command.electionId()));
        BallotStyle ballotStyle = ballotStyleRepository
                .findById(command.ballotStyleId())
                .orElseThrow(() -> new EntityNotFoundException("BallotStyle not found: " + command.ballotStyleId()));

        OffsetDateTime openedAt = clock.get();
        validateOpeningPolicies(voterRecord, election, ballotStyle, command, openedAt);
        VotingSession session = voterRecord.openVotingSession(
                idGenerator.get(), election, ballotStyle, command.deviceId(), openedAt, command.votingMethod());
        try {
            VotingSession saved = votingSessionRepository.save(session);
            publishAuditEvent(
                    "VotingSessionOpened",
                    saved.getId(),
                    "VotingSession",
                    Map.of(
                            "voterId", voterRecord.getId().toString(),
                            "electionId", election.getId().toString(),
                            "ballotStyleId", ballotStyle.getId().toString(),
                            "deviceId", saved.getDeviceId(),
                            "votingMethod", saved.getVotingMethod().name(),
                            "sessionStatus", saved.getSessionStatus().name()),
                    command.actorId(),
                    command.sourceIp());
            return saved;
        } catch (DataIntegrityViolationException exception) {
            throw duplicateVoteException(command.voterId(), command.electionId(), exception);
        }
    }

    public CastBallotReceipt castBallot(CastBallotCommand command) {
        VotingSession session = findSession(command.sessionId());
        if (session.getSessionStatus() != SessionStatus.OPENED) {
            throw new VotingSessionValidationException("VotingSession must be OPENED before casting a ballot");
        }
        OffsetDateTime castAt = clock.get();
        List<VotingResult> results = command.selections().stream()
                .map(selection -> VotingResult.create(
                        idGenerator.get(), session, findContest(selection.contestId()), selection.selectedCandidateIds(), castAt))
                .toList();
        Iterable<VotingResult> savedIterable = votingResultRepository.saveAll(results);
        List<VotingResult> savedResults = new ArrayList<>();
        savedIterable.forEach(savedResults::add);
        session.cast(castAt);
        votingSessionRepository.save(session);
        List<String> hashes = savedResults.stream().map(VotingResult::getHash).toList();
        publishAuditEvent(
                "VoteCastEvent",
                session.getId(),
                "VotingSession",
                Map.of(
                        "sessionId", session.getId().toString(),
                        "electionId", session.getElection().getId().toString(),
                        "resultCount", savedResults.size(),
                        "resultHashes", hashes),
                command.actorId(),
                command.sourceIp());
        return new CastBallotReceipt(session.getId(), hashes);
    }

    public VotingSession spoilBallot(UUID sessionId, String actorId, String sourceIp) {
        VotingSession session = findSession(sessionId);
        session.spoil(clock.get());
        VotingSession saved = votingSessionRepository.save(session);
        publishAuditEvent(
                "VotingSessionSpoiled",
                saved.getId(),
                "VotingSession",
                Map.of(
                        "sessionId", saved.getId().toString(),
                        "electionId", saved.getElection().getId().toString(),
                        "sessionStatus", saved.getSessionStatus().name()),
                actorId,
                sourceIp);
        return saved;
    }

    public UUID electionIdForSession(UUID sessionId) {
        return findSession(sessionId).getElection().getId();
    }

    private void ensureNoExistingNonSpoiledSession(UUID voterId, UUID electionId) {
        if (votingSessionRepository.existsByVoterRecordIdAndElectionIdAndSessionStatusNot(
                voterId, electionId, SessionStatus.SPOILED)) {
            throw duplicateVoteException(voterId, electionId, null);
        }
    }

    private void validateOpeningPolicies(
            VoterRecord voterRecord,
            Election election,
            BallotStyle ballotStyle,
            OpenSessionCommand command,
            OffsetDateTime openedAt) {
        VotingSessionOpeningContext context = new VotingSessionOpeningContext(
                voterRecord,
                election,
                ballotStyle,
                command.votingMethod(),
                openedAt,
                command.homeDistrictCode(),
                command.pollingStationDistrictCode());
        openingPolicies.forEach(policy -> policy.validate(context));
    }

    private VotingSession findSession(UUID sessionId) {
        return votingSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("VotingSession not found: " + sessionId));
    }

    private Contest findContest(UUID contestId) {
        return contestRepository
                .findById(contestId)
                .orElseThrow(() -> new EntityNotFoundException("Contest not found: " + contestId));
    }

    private VotingSessionValidationException duplicateVoteException(UUID voterId, UUID electionId, Exception cause) {
        VotingSessionValidationException exception = new VotingSessionValidationException(
                "duplicate voting session rejected for voter " + voterId + " and election " + electionId);
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    private void publishAuditEvent(
            String eventType,
            UUID aggregateId,
            String aggregateType,
            Map<String, Object> payload,
            String actorId,
            String sourceIp) {
        applicationEventPublisher.publishEvent(
                new TransactionalAuditEvent(eventType, aggregateId, aggregateType, payload, actorId, sourceIp));
    }

    public record OpenSessionCommand(
            UUID voterId,
            UUID electionId,
            UUID ballotStyleId,
            String deviceId,
            String actorId,
            String sourceIp,
            VotingMethod votingMethod,
            String homeDistrictCode,
            String pollingStationDistrictCode) {
        public OpenSessionCommand(
                UUID voterId, UUID electionId, UUID ballotStyleId, String deviceId, String actorId, String sourceIp) {
            this(voterId, electionId, ballotStyleId, deviceId, actorId, sourceIp, VotingMethod.ELECTION_DAY);
        }

        public OpenSessionCommand(
                UUID voterId,
                UUID electionId,
                UUID ballotStyleId,
                String deviceId,
                String actorId,
                String sourceIp,
                VotingMethod votingMethod) {
            this(voterId, electionId, ballotStyleId, deviceId, actorId, sourceIp, votingMethod, null, null);
        }
    }

    public record ContestSelection(UUID contestId, List<UUID> selectedCandidateIds) {}

    public record CastBallotCommand(
            UUID sessionId, List<ContestSelection> selections, String actorId, String sourceIp) {
        public CastBallotCommand {
            selections = List.copyOf(selections);
        }
    }

    public record CastBallotReceipt(UUID sessionId, List<String> resultHashes) {
        public CastBallotReceipt {
            resultHashes = List.copyOf(resultHashes);
        }
    }
}
