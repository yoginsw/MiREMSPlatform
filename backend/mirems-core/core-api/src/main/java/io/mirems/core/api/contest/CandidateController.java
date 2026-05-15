package io.mirems.core.api.contest;

import io.mirems.core.api.generated.api.CandidatesApi;
import io.mirems.core.api.generated.model.CandidateRequest;
import io.mirems.core.api.generated.model.CandidateResponse;
import io.mirems.core.bpmn.candidate.CandidateOfficerDecision;
import io.mirems.core.bpmn.candidate.CandidateRegistrationProcessService;
import io.mirems.core.bpmn.candidate.CandidateRegistrationRequest;
import io.mirems.core.domain.contest.Candidate;
import io.mirems.core.infra.service.election.ElectionManagementService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
public class CandidateController implements CandidatesApi {
    private final ObjectProvider<ElectionManagementService> electionManagementService;
    private final ObjectProvider<CandidateRegistrationProcessService> candidateRegistrationProcessService;
    private final HttpServletRequest httpServletRequest;

    public CandidateController(
            ObjectProvider<ElectionManagementService> electionManagementService,
            ObjectProvider<CandidateRegistrationProcessService> candidateRegistrationProcessService,
            HttpServletRequest httpServletRequest) {
        this.electionManagementService = electionManagementService;
        this.candidateRegistrationProcessService = candidateRegistrationProcessService;
        this.httpServletRequest = httpServletRequest;
    }

    @PreAuthorize("hasAnyRole('ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<CandidateResponse> registerCandidate(
            UUID electionId, UUID contestId, CandidateRequest candidateRequest) {
        Candidate candidate = service().addCandidate(new ElectionManagementService.AddCandidateCommand(
                contestId,
                candidateRequest.getDisplayName(),
                candidateRequest.getParty(),
                actorId(),
                sourceIp()));
        processService().register(new CandidateRegistrationRequest(
                candidate,
                35,
                true,
                reviewerRole(),
                CandidateOfficerDecision.PENDING,
                OffsetDateTime.now()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toResponse(candidate));
    }

    @PreAuthorize("hasAnyRole('OBSERVER','ELECTION_OFFICER','AUDITOR','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<List<CandidateResponse>> listCandidates(UUID electionId, UUID contestId) {
        return ResponseEntity.ok(service().listCandidates(electionId, contestId).stream().map(this::toResponse).toList());
    }

    @PreAuthorize("hasAnyRole('OBSERVER','ELECTION_OFFICER','AUDITOR','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<CandidateResponse> getCandidate(UUID electionId, UUID contestId, UUID candidateId) {
        return ResponseEntity.ok(toResponse(service()
                .getCandidate(electionId, contestId, candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId))));
    }

    @PreAuthorize("hasAnyRole('ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<CandidateResponse> withdrawCandidate(UUID electionId, UUID contestId, UUID candidateId) {
        return ResponseEntity.ok(toResponse(service().withdrawCandidate(candidateId, actorId(), sourceIp())));
    }

    private ElectionManagementService service() {
        ElectionManagementService service = electionManagementService.getIfAvailable();
        if (service == null) {
            throw new CandidateServiceUnavailableException();
        }
        return service;
    }

    private CandidateRegistrationProcessService processService() {
        CandidateRegistrationProcessService processService = candidateRegistrationProcessService.getIfAvailable();
        if (processService == null) {
            throw new CandidateRegistrationUnavailableException();
        }
        return processService;
    }

    CandidateResponse toResponse(Candidate candidate) {
        return new CandidateResponse(
                        candidate.getId(),
                        candidate.getContest().getId(),
                        candidate.getName(),
                        io.mirems.core.api.generated.model.CandidateStatus.fromValue(
                                candidate.getCandidateStatus().name()))
                .party(candidate.getPartyAffiliation());
    }

    private String actorId() {
        if (httpServletRequest.getUserPrincipal() == null) {
            return "anonymous";
        }
        return httpServletRequest.getUserPrincipal().getName();
    }

    private String sourceIp() {
        String forwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return httpServletRequest.getRemoteAddr();
    }

    private String reviewerRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.equals("ROLE_ELECTION_OFFICER") || authority.equals("ROLE_ELECTION_ADMIN"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("UNKNOWN");
    }

    static class CandidateNotFoundException extends RuntimeException {
        CandidateNotFoundException(UUID candidateId) {
            super("Candidate not found: " + candidateId);
        }
    }

    static class CandidateServiceUnavailableException extends RuntimeException {
        CandidateServiceUnavailableException() {
            super("Candidate management service is unavailable");
        }
    }

    static class CandidateRegistrationUnavailableException extends RuntimeException {
        CandidateRegistrationUnavailableException() {
            super("Candidate registration process is unavailable");
        }
    }
}
