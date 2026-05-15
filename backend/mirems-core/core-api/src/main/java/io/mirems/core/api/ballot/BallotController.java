package io.mirems.core.api.ballot;

import io.mirems.core.api.generated.api.BallotsApi;
import io.mirems.core.api.generated.model.BallotContestResponse;
import io.mirems.core.api.generated.model.BallotPreviewResponse;
import io.mirems.core.api.generated.model.BallotRequest;
import io.mirems.core.api.generated.model.BallotResponse;
import io.mirems.core.api.generated.model.BallotStyleRequest;
import io.mirems.core.api.generated.model.BallotStyleResponse;
import io.mirems.core.api.generated.model.BallotVersionRequest;
import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotContest;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.infra.service.ballot.BallotManagementService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.Principal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Validated
public class BallotController implements BallotsApi {
    private final ObjectProvider<BallotManagementService> ballotManagementService;
    private final HttpServletRequest request;

    public BallotController(ObjectProvider<BallotManagementService> ballotManagementService, HttpServletRequest request) {
        this.ballotManagementService = ballotManagementService;
        this.request = request;
    }

    @PreAuthorize("hasAnyRole('ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<BallotResponse> createBallot(UUID electionId, BallotRequest ballotRequest) {
        Ballot ballot = service().createBallot(new BallotManagementService.CreateBallotCommand(
                electionId,
                ballotRequest.getContestIds(),
                actorId(),
                sourceIp()));
        return ResponseEntity.created(URI.create("/elections/" + electionId + "/ballots/" + ballot.getId()))
                .body(toBallotResponse(ballot));
    }

    @PreAuthorize("hasAnyRole('OBSERVER','ELECTION_OFFICER','AUDITOR','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<List<BallotResponse>> listBallots(UUID electionId) {
        return ResponseEntity.ok(service().listBallots(electionId).stream().map(this::toBallotResponse).toList());
    }

    @PreAuthorize("hasAnyRole('ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<BallotResponse> createBallotVersion(
            UUID electionId, UUID ballotId, BallotVersionRequest ballotVersionRequest) {
        Ballot ballot = service().createBallotVersion(new BallotManagementService.CreateBallotVersionCommand(
                electionId,
                ballotId,
                ballotVersionRequest.getChangeReason(),
                ballotVersionRequest.getContestIds(),
                actorId(),
                sourceIp()));
        return ResponseEntity.created(URI.create("/elections/" + electionId + "/ballots/" + ballotId + "/versions/"
                        + ballot.getBallotVersion()))
                .body(toBallotResponse(ballot));
    }

    @PreAuthorize("hasAnyRole('OBSERVER','ELECTION_OFFICER','AUDITOR','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<BallotPreviewResponse> previewBallot(UUID electionId, UUID ballotId) {
        Ballot ballot = service().previewBallot(electionId, ballotId)
                .orElseThrow(() -> new BallotNotFoundException("Ballot not found: " + ballotId));
        return ResponseEntity.ok(new BallotPreviewResponse(ballot.getId(), previewLayout(ballot)));
    }

    @PreAuthorize("hasAnyRole('ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<BallotStyleResponse> createBallotStyle(UUID electionId, BallotStyleRequest ballotStyleRequest) {
        BallotStyle style = service().createBallotStyle(new BallotManagementService.CreateBallotStyleCommand(
                electionId,
                ballotStyleRequest.getBallotId(),
                ballotStyleRequest.getStyleCode(),
                ballotStyleRequest.getDistrict(),
                ballotStyleRequest.getLanguage(),
                toDomainAccessibilityFeatures(ballotStyleRequest.getAccessibilityFeatures()),
                actorId(),
                sourceIp()));
        return ResponseEntity.created(URI.create("/elections/" + electionId + "/ballot-styles/" + style.getId()))
                .body(toBallotStyleResponse(style));
    }

    @PreAuthorize("hasAnyRole('OBSERVER','ELECTION_OFFICER','AUDITOR','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<List<BallotStyleResponse>> listBallotStyles(UUID electionId) {
        return ResponseEntity.ok(service().listBallotStyles(electionId).stream().map(this::toBallotStyleResponse).toList());
    }

    @PreAuthorize("hasAnyRole('ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<BallotStyleResponse> updateBallotStyle(
            UUID electionId, UUID ballotStyleId, BallotStyleRequest ballotStyleRequest) {
        BallotStyle style = service().updateBallotStyle(new BallotManagementService.UpdateBallotStyleCommand(
                electionId,
                ballotStyleId,
                ballotStyleRequest.getBallotId(),
                ballotStyleRequest.getStyleCode(),
                ballotStyleRequest.getDistrict(),
                ballotStyleRequest.getLanguage(),
                toDomainAccessibilityFeatures(ballotStyleRequest.getAccessibilityFeatures()),
                actorId(),
                sourceIp()));
        return ResponseEntity.ok(toBallotStyleResponse(style));
    }

    @PreAuthorize("hasAnyRole('ELECTION_OFFICER','ELECTION_ADMIN','SYSTEM_ADMIN')")
    @Override
    public ResponseEntity<Void> deleteBallotStyle(UUID electionId, UUID ballotStyleId) {
        service().deleteBallotStyle(electionId, ballotStyleId, actorId(), sourceIp());
        return ResponseEntity.noContent().build();
    }

    private BallotManagementService service() {
        BallotManagementService service = ballotManagementService.getIfAvailable();
        if (service == null) {
            throw new BallotServiceUnavailableException("Ballot management service is unavailable");
        }
        return service;
    }

    private BallotResponse toBallotResponse(Ballot ballot) {
        return new BallotResponse(
                ballot.getId(),
                ballot.getElection().getId(),
                ballot.getBallotVersion(),
                ballot.isActive(),
                ballot.getBallotContests().stream()
                        .sorted(Comparator.comparingInt(BallotContest::getDisplayOrder))
                        .map(contest -> new BallotContestResponse(
                                contest.getContest().getId(),
                                contest.getDisplayOrder(),
                                contest.getPresentationTitle()))
                        .toList(),
                ballot.getBallotStyles().stream().map(this::toBallotStyleResponse).toList());
    }

    private BallotStyleResponse toBallotStyleResponse(BallotStyle style) {
        return new BallotStyleResponse(
                style.getId(),
                style.getBallot().getId(),
                style.getStyleCode(),
                style.getDistrict(),
                style.getLanguage(),
                style.getAccessibilityFeatures().stream()
                        .map(feature -> io.mirems.core.api.generated.model.AccessibilityFeature.fromValue(feature.name()))
                        .toList());
    }

    private Set<AccessibilityFeature> toDomainAccessibilityFeatures(
            List<io.mirems.core.api.generated.model.AccessibilityFeature> features) {
        return features.stream()
                .map(feature -> AccessibilityFeature.valueOf(feature.getValue()))
                .collect(Collectors.toSet());
    }

    private Map<String, Object> previewLayout(Ballot ballot) {
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("version", ballot.getBallotVersion());
        layout.put("active", ballot.isActive());
        layout.put("contests", ballot.getBallotContests().stream()
                .sorted(Comparator.comparingInt(BallotContest::getDisplayOrder))
                .map(contest -> Map.of(
                        "contestId", contest.getContest().getId().toString(),
                        "order", contest.getDisplayOrder(),
                        "title", contest.getPresentationTitle()))
                .toList());
        layout.put("styles", ballot.getBallotStyles().stream()
                .map(style -> Map.of(
                        "styleCode", style.getStyleCode(),
                        "district", style.getDistrict(),
                        "language", style.getLanguage()))
                .toList());
        return layout;
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

    public static final class BallotNotFoundException extends RuntimeException {
        public BallotNotFoundException(String message) {
            super(message);
        }
    }

    public static final class BallotServiceUnavailableException extends RuntimeException {
        public BallotServiceUnavailableException(String message) {
            super(message);
        }
    }
}
