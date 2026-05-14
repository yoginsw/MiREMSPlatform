package io.mirems.core.api.election;

import io.mirems.core.api.generated.api.ElectionsApi;
import io.mirems.core.api.generated.model.ElectionRequest;
import io.mirems.core.api.generated.model.ElectionResponse;
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
public class ElectionController implements ElectionsApi {
    private final ObjectProvider<ElectionManagementService> electionManagementService;
    private final HttpServletRequest httpServletRequest;

    public ElectionController(
            ObjectProvider<ElectionManagementService> electionManagementService, HttpServletRequest httpServletRequest) {
        this.electionManagementService = electionManagementService;
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public ResponseEntity<ElectionResponse> createElection(ElectionRequest electionRequest) {
        io.mirems.core.domain.election.Election election = service().createElection(
                new ElectionManagementService.CreateElectionCommand(
                        electionRequest.getName(),
                        toDomainType(electionRequest.getElectionType()),
                        electionRequest.getJurisdiction(),
                        electionRequest.getScheduledDate(),
                        electionRequest.getCountryCode(),
                        electionRequest.getExtensionPackId(),
                        actorId(),
                        sourceIp()));
        return ResponseEntity
                .created(URI.create("/elections/" + election.getId()))
                .body(toResponse(election));
    }

    @Override
    public ResponseEntity<List<ElectionResponse>> listElections() {
        return ResponseEntity.ok(service().listElections().stream()
                .map(this::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<ElectionResponse> getElection(UUID electionId) {
        return ResponseEntity.ok(toResponse(service()
                .getElection(electionId)
                .orElseThrow(() -> new ElectionNotFoundException(electionId))));
    }

    @Override
    public ResponseEntity<ElectionResponse> publishElection(UUID electionId) {
        return ResponseEntity.ok(toResponse(service().publishElection(electionId, actorId(), sourceIp())));
    }

    @Override
    public ResponseEntity<ElectionResponse> closeElection(UUID electionId) {
        return ResponseEntity.ok(toResponse(service().closeElection(electionId, actorId(), sourceIp())));
    }

    private ElectionManagementService service() {
        ElectionManagementService service = electionManagementService.getIfAvailable();
        if (service == null) {
            throw new ElectionServiceUnavailableException();
        }
        return service;
    }

    private ElectionResponse toResponse(io.mirems.core.domain.election.Election election) {
        return new ElectionResponse(
                election.getId(),
                election.getName(),
                io.mirems.core.api.generated.model.ElectionType.fromValue(election.getElectionType().name()),
                election.getJurisdiction(),
                io.mirems.core.api.generated.model.ElectionStatus.fromValue(election.getElectionStatus().name()),
                election.getScheduledDate(),
                election.getCountryCode(),
                election.getExtensionPackId());
    }

    private io.mirems.core.domain.election.ElectionType toDomainType(
            io.mirems.core.api.generated.model.ElectionType electionType) {
        return io.mirems.core.domain.election.ElectionType.valueOf(electionType.name());
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

    static class ElectionNotFoundException extends RuntimeException {
        ElectionNotFoundException(UUID electionId) {
            super("Election not found: " + electionId);
        }
    }

    static class ElectionServiceUnavailableException extends RuntimeException {
        ElectionServiceUnavailableException() {
            super("Election management service is unavailable");
        }
    }
}
