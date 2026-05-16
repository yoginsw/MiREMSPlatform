package io.mirems.extension.us.provisional;

import io.mirems.extension.us.rules.UsVoterEligibilityDecisionService;
import io.mirems.extension.us.rules.UsVoterEligibilityRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UsProvisionalBallotWorkflowService {
    private final UsVoterEligibilityDecisionService eligibilityDecisionService;

    public UsProvisionalBallotWorkflowService() {
        this(new UsVoterEligibilityDecisionService());
    }

    public UsProvisionalBallotWorkflowService(UsVoterEligibilityDecisionService eligibilityDecisionService) {
        this.eligibilityDecisionService = eligibilityDecisionService;
    }

    public UsProvisionalBallot createFromEligibility(
            UUID voterId,
            UUID electionId,
            UUID ballotId,
            UsVoterEligibilityRequest eligibilityRequest,
            OffsetDateTime castAt) {
        var eligibility = eligibilityDecisionService.evaluate(eligibilityRequest);
        if (!eligibility.provisionalBallotRequired()) {
            throw new IllegalArgumentException("eligibility result does not require a provisional ballot");
        }
        return createManual(voterId, electionId, ballotId, mapReason(eligibility.reason()), castAt);
    }

    public UsProvisionalBallot createManual(UUID voterId, UUID electionId, UUID ballotId, String reasonCode, OffsetDateTime castAt) {
        String normalizedReason = UsProvisionalBallot.normalizeReason(reasonCode);
        UUID id = UUID.nameUUIDFromBytes((voterId + ":" + electionId + ":" + ballotId + ":" + normalizedReason).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new UsProvisionalBallot(
                id,
                voterId,
                electionId,
                ballotId,
                normalizedReason,
                UsProvisionalBallotStatus.PENDING_REVIEW,
                castAt,
                Optional.empty(),
                Optional.empty(),
                List.of("CREATED:" + normalizedReason));
    }

    public UsProvisionalBallot resolve(UsProvisionalBallot ballot, boolean accepted, String resolvedBy, OffsetDateTime resolvedAt) {
        if (ballot.resolved()) {
            throw new IllegalStateException("provisional ballot is already resolved");
        }
        String reviewer = java.util.Objects.requireNonNull(resolvedBy, "resolvedBy is required").trim();
        if (reviewer.isEmpty()) {
            throw new IllegalArgumentException("resolvedBy is required");
        }
        UsProvisionalBallotStatus status = accepted ? UsProvisionalBallotStatus.ACCEPTED : UsProvisionalBallotStatus.REJECTED;
        List<String> auditTrail = new ArrayList<>(ballot.auditTrail());
        auditTrail.add("RESOLVED:" + status);
        return new UsProvisionalBallot(
                ballot.id(),
                ballot.voterId(),
                ballot.electionId(),
                ballot.ballotId(),
                ballot.reasonCode(),
                status,
                ballot.castAt(),
                Optional.of(resolvedAt),
                Optional.of(reviewer),
                auditTrail);
    }

    private static String mapReason(String reason) {
        if (reason != null && reason.startsWith("HAVA ID verification")) {
            return "HAVA_ID_UNVERIFIED";
        }
        return reason;
    }
}
