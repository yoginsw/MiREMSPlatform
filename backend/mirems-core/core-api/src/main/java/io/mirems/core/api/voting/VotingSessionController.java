package io.mirems.core.api.voting;

import io.mirems.core.api.generated.api.VotingSessionsApi;
import io.mirems.core.api.generated.model.VoteCastReceiptResponse;
import io.mirems.core.api.generated.model.VoteCastRequest;
import io.mirems.core.api.generated.model.VoteSelection;
import io.mirems.core.api.generated.model.VotingSessionRequest;
import io.mirems.core.api.generated.model.VotingSessionResponse;
import io.mirems.core.api.security.ElectionScopeValidator;
import io.mirems.core.domain.voting.VotingSession;
import io.mirems.core.infra.service.voting.VotingSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
public class VotingSessionController implements VotingSessionsApi {
    private final ObjectProvider<VotingSessionService> votingSessionService;
    private final HttpServletRequest request;
    private final ElectionScopeValidator electionScopeValidator;

    public VotingSessionController(
            ObjectProvider<VotingSessionService> votingSessionService,
            HttpServletRequest request,
            ElectionScopeValidator electionScopeValidator) {
        this.votingSessionService = votingSessionService;
        this.request = request;
        this.electionScopeValidator = electionScopeValidator;
    }

    @PreAuthorize("hasAnyRole('VOTER','ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<VotingSessionResponse> createVotingSession(VotingSessionRequest votingSessionRequest) {
        ensureCanAccessVoter(votingSessionRequest.getVoterId());
        electionScopeValidator.requireAccess(votingSessionRequest.getElectionId());
        VotingSession session = service().openSession(new VotingSessionService.OpenSessionCommand(
                votingSessionRequest.getVoterId(),
                votingSessionRequest.getElectionId(),
                votingSessionRequest.getBallotStyleId(),
                votingSessionRequest.getDeviceId(),
                actorId(),
                sourceIp()));
        return ResponseEntity.created(URI.create("/sessions/" + session.getId())).body(toResponse(session));
    }

    @PreAuthorize("hasAnyRole('VOTER','ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<VoteCastReceiptResponse> castVote(UUID sessionId, VoteCastRequest voteCastRequest) {
        electionScopeValidator.requireAccess(service().electionIdForSession(sessionId));
        VotingSessionService.CastBallotReceipt receipt = service().castBallot(new VotingSessionService.CastBallotCommand(
                sessionId,
                voteCastRequest.getSelections().stream().map(this::toSelection).toList(),
                actorId(),
                sourceIp()));
        return ResponseEntity.created(URI.create("/sessions/" + sessionId + "/receipt"))
                .body(new VoteCastReceiptResponse(receipt.sessionId(), receipt.resultHashes(), receiptHash(receipt.resultHashes())));
    }

    @PreAuthorize("hasAnyRole('VOTER','ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<VotingSessionResponse> spoilVotingSession(UUID sessionId) {
        electionScopeValidator.requireAccess(service().electionIdForSession(sessionId));
        VotingSession session = service().spoilBallot(sessionId, actorId(), sourceIp());
        return ResponseEntity.ok(toResponse(session));
    }

    private VotingSessionService.ContestSelection toSelection(VoteSelection selection) {
        return new VotingSessionService.ContestSelection(selection.getContestId(), List.copyOf(selection.getSelectionIds()));
    }

    private VotingSessionResponse toResponse(VotingSession session) {
        return new VotingSessionResponse(
                session.getId(),
                session.getVoterRecord().getId(),
                session.getElection().getId(),
                session.getBallotStyle().getId(),
                VotingSessionResponse.StatusEnum.fromValue(session.getSessionStatus().name()));
    }

    private String receiptHash(List<String> hashes) {
        if (hashes.isEmpty()) {
            throw new IllegalArgumentException("resultHashes must contain at least one hash");
        }
        if (hashes.size() == 1) {
            return hashes.getFirst();
        }
        return String.join(":", hashes);
    }

    private VotingSessionService service() {
        VotingSessionService service = votingSessionService.getIfAvailable();
        if (service == null) {
            throw new VotingSessionServiceUnavailableException("Voting session service is unavailable");
        }
        return service;
    }

    private void ensureCanAccessVoter(UUID voterId) {
        if (hasRole("ELECTION_OFFICER")) {
            return;
        }
        if (hasRole("VOTER") && voterId.toString().equals(actorId())) {
            return;
        }
        throw new AccessDeniedException("Voting session voter access denied");
    }

    private boolean hasRole(String role) {
        Principal principal = request.getUserPrincipal();
        if (principal instanceof Authentication authentication) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals("ROLE_" + role));
        }
        return false;
    }

    private String actorId() {
        Principal principal = request.getUserPrincipal();
        return principal == null ? "anonymous" : principal.getName();
    }

    private String sourceIp() {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static final class VotingSessionServiceUnavailableException extends RuntimeException {
        public VotingSessionServiceUnavailableException(String message) {
            super(message);
        }
    }
}
