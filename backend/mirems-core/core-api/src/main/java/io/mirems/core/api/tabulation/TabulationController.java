package io.mirems.core.api.tabulation;

import io.mirems.core.api.generated.api.TabulationApi;
import io.mirems.core.api.generated.model.CandidateTally;
import io.mirems.core.api.generated.model.ContestTally;
import io.mirems.core.api.generated.model.ProcessStatus;
import io.mirems.core.api.generated.model.TabulationResultResponse;
import io.mirems.core.api.security.ElectionScopeValidator;
import io.mirems.core.api.security.ElectionScoped;
import io.mirems.core.bpmn.tabulation.BallotTabulationProcessService;
import io.mirems.core.bpmn.tabulation.BallotTabulationRequest;
import io.mirems.core.bpmn.tabulation.BallotTabulationResult;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionRepository;
import io.mirems.core.domain.election.ElectionStatus;
import io.mirems.core.domain.result.TabulationReport;
import io.mirems.core.domain.result.TabulationReportRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
public class TabulationController implements TabulationApi {
    private final ObjectProvider<BallotTabulationProcessService> tabulationProcessService;
    private final ObjectProvider<ElectionRepository> electionRepository;
    private final ObjectProvider<TabulationReportRepository> tabulationReportRepository;
    private final HttpServletRequest request;
    private final ElectionScopeValidator electionScopeValidator;

    public TabulationController(
            ObjectProvider<BallotTabulationProcessService> tabulationProcessService,
            ObjectProvider<ElectionRepository> electionRepository,
            ObjectProvider<TabulationReportRepository> tabulationReportRepository,
            HttpServletRequest request,
            ElectionScopeValidator electionScopeValidator) {
        this.tabulationProcessService = tabulationProcessService;
        this.electionRepository = electionRepository;
        this.tabulationReportRepository = tabulationReportRepository;
        this.request = request;
        this.electionScopeValidator = electionScopeValidator;
    }

    @PreAuthorize("hasAnyRole('TABULATION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @ElectionScoped
    @Override
    public ResponseEntity<ProcessStatus> tabulateElection(UUID electionId) {
        Election election = findElection(electionId);
        BallotTabulationResult result = tabulationService().tabulate(new BallotTabulationRequest(
                UUID.randomUUID(),
                electionId,
                election.getElectionStatus(),
                reviewerRole(),
                election.getElectionStatus() == ElectionStatus.CERTIFIED));
        TabulationReport report = result.report();
        ProcessStatus status = new ProcessStatus(
                report.getId().toString(),
                "BallotTabulationProcess",
                result.completed() ? "COMPLETED" : "ACTIVE",
                Map.of(
                        "electionId", electionId.toString(),
                        "reportId", report.getId().toString(),
                        "reportHash", report.getHash(),
                        "published", result.published()),
                List.of());
        return ResponseEntity.accepted()
                .location(URI.create("/elections/" + electionId + "/results"))
                .body(status);
    }

    @Override
    public ResponseEntity<TabulationResultResponse> getElectionResults(UUID electionId) {
        Election election = findElection(electionId);
        if (election.getElectionStatus() != ElectionStatus.CERTIFIED) {
            requireAuthenticatedOfficial();
            electionScopeValidator.requireAccess(electionId);
        }
        TabulationReport report = reportRepository()
                .findByElectionId(electionId)
                .orElseThrow(() -> new TabulationReportNotFoundException("Tabulation report not found for election: " + electionId));
        return ResponseEntity.ok(toResponse(election, report));
    }

    private TabulationResultResponse toResponse(Election election, TabulationReport report) {
        TabulationResultResponse.StatusEnum status = election.getElectionStatus() == ElectionStatus.CERTIFIED
                ? TabulationResultResponse.StatusEnum.CERTIFIED
                : TabulationResultResponse.StatusEnum.COMPLETED;
        return new TabulationResultResponse(
                report.getElectionId(),
                status,
                report.getGeneratedAt(),
                report.getContestTallies().values().stream()
                        .sorted(Comparator.comparing(io.mirems.core.domain.result.ContestTally::contestId))
                        .map(this::toContestTally)
                        .toList());
    }

    private ContestTally toContestTally(io.mirems.core.domain.result.ContestTally tally) {
        return new ContestTally(
                tally.contestId(),
                tally.candidateTallies().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> new CandidateTally(entry.getKey(), entry.getValue().longValue()))
                        .toList());
    }

    private Election findElection(UUID electionId) {
        return electionRepository().findById(electionId)
                .orElseThrow(() -> new EntityNotFoundException("Election not found: " + electionId));
    }

    private BallotTabulationProcessService tabulationService() {
        BallotTabulationProcessService service = tabulationProcessService.getIfAvailable();
        if (service == null) {
            throw new TabulationServiceUnavailableException("Tabulation process service is unavailable");
        }
        return service;
    }

    private ElectionRepository electionRepository() {
        ElectionRepository repository = electionRepository.getIfAvailable();
        if (repository == null) {
            throw new TabulationServiceUnavailableException("Election repository is unavailable");
        }
        return repository;
    }

    private TabulationReportRepository reportRepository() {
        TabulationReportRepository repository = tabulationReportRepository.getIfAvailable();
        if (repository == null) {
            throw new TabulationServiceUnavailableException("Tabulation report repository is unavailable");
        }
        return repository;
    }

    private void requireAuthenticatedOfficial() {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required to view uncertified results");
        }
        if (hasRole("ELECTION_OFFICER") || hasRole("ELECTION_ADMIN") || hasRole("TABULATION_OFFICER")) {
            return;
        }
        throw new AccessDeniedException("Election results are restricted until certification");
    }

    private String reviewerRole() {
        if (hasRole("TABULATION_OFFICER")) {
            return "TABULATION_OFFICER";
        }
        throw new AccessDeniedException("TABULATION_OFFICER role is required");
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

    static final class TabulationReportNotFoundException extends RuntimeException {
        TabulationReportNotFoundException(String message) {
            super(message);
        }
    }

    static final class TabulationServiceUnavailableException extends RuntimeException {
        TabulationServiceUnavailableException(String message) {
            super(message);
        }
    }
}
