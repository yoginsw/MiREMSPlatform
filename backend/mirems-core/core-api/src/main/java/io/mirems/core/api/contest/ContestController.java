package io.mirems.core.api.contest;

import io.mirems.core.api.generated.api.ContestsApi;
import io.mirems.core.api.generated.model.ContestRequest;
import io.mirems.core.api.generated.model.ContestResponse;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.infra.service.election.ElectionManagementService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class ContestController implements ContestsApi {
    private final ObjectProvider<ElectionManagementService> electionManagementService;
    private final HttpServletRequest httpServletRequest;

    public ContestController(
            ObjectProvider<ElectionManagementService> electionManagementService, HttpServletRequest httpServletRequest) {
        this.electionManagementService = electionManagementService;
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public ResponseEntity<ContestResponse> createContest(UUID electionId, ContestRequest contestRequest) {
        Contest contest = service().addContest(new ElectionManagementService.AddContestCommand(
                electionId,
                toDomainType(contestRequest.getType()),
                contestRequest.getTitle(),
                contestRequest.getSeats(),
                contestRequest.getSeats(),
                actorId(),
                sourceIp()));
        return ResponseEntity
                .created(URI.create("/elections/" + electionId + "/contests/" + contest.getId()))
                .body(toResponse(contest));
    }

    @Override
    public ResponseEntity<List<ContestResponse>> listContests(UUID electionId) {
        return ResponseEntity.ok(service().listContests(electionId).stream().map(this::toResponse).toList());
    }

    @Override
    public ResponseEntity<ContestResponse> getContest(UUID electionId, UUID contestId) {
        return ResponseEntity.ok(toResponse(service()
                .getContest(electionId, contestId)
                .orElseThrow(() -> new ContestNotFoundException(contestId))));
    }

    @Override
    public ResponseEntity<ContestResponse> updateContest(UUID electionId, UUID contestId, ContestRequest contestRequest) {
        Contest contest = service().updateContest(new ElectionManagementService.UpdateContestCommand(
                electionId,
                contestId,
                toDomainType(contestRequest.getType()),
                contestRequest.getTitle(),
                contestRequest.getSeats(),
                contestRequest.getSeats(),
                actorId(),
                sourceIp()));
        return ResponseEntity.ok(toResponse(contest));
    }

    private ElectionManagementService service() {
        ElectionManagementService service = electionManagementService.getIfAvailable();
        if (service == null) {
            throw new ContestServiceUnavailableException();
        }
        return service;
    }

    ContestResponse toResponse(Contest contest) {
        return new ContestResponse(
                contest.getId(),
                contest.getElection().getId(),
                contest.getName(),
                io.mirems.core.api.generated.model.ContestType.fromValue(contest.getContestType().name()),
                contest.getSeats());
    }

    private io.mirems.core.domain.contest.ContestType toDomainType(
            io.mirems.core.api.generated.model.ContestType contestType) {
        return io.mirems.core.domain.contest.ContestType.valueOf(contestType.name());
    }

    private String actorId() {
        Principal principal = httpServletRequest.getUserPrincipal();
        return principal == null ? "anonymous" : principal.getName();
    }

    private String sourceIp() {
        String forwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return httpServletRequest.getRemoteAddr();
    }

    static class ContestNotFoundException extends RuntimeException {
        ContestNotFoundException(UUID contestId) {
            super("Contest not found: " + contestId);
        }
    }

    static class ContestServiceUnavailableException extends RuntimeException {
        ContestServiceUnavailableException() {
            super("Contest management service is unavailable");
        }
    }
}
