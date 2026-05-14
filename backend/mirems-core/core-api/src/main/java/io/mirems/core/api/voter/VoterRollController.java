package io.mirems.core.api.voter;

import io.mirems.core.api.generated.api.VotersApi;
import io.mirems.core.api.generated.model.VoterEligibilityResponse;
import io.mirems.core.api.generated.model.VoterMaskedResponse;
import io.mirems.core.api.generated.model.VoterRegistrationRequest;
import io.mirems.core.bpmn.voter.VoterEligibilityResult;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.core.infra.service.voting.VoterRollService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class VoterRollController implements VotersApi {
    private final ObjectProvider<VoterRollService> voterRollService;
    private final ObjectProvider<PiiEncryptionService> encryptionService;
    private final HttpServletRequest request;

    public VoterRollController(
            ObjectProvider<VoterRollService> voterRollService,
            ObjectProvider<PiiEncryptionService> encryptionService,
            HttpServletRequest request) {
        this.voterRollService = voterRollService;
        this.encryptionService = encryptionService;
        this.request = request;
    }

    @Override
    public ResponseEntity<VoterMaskedResponse> registerVoter(VoterRegistrationRequest voterRegistrationRequest) {
        VoterRecord voter = service().registerVoter(new VoterRollService.RegisterVoterCommand(
                voterRegistrationRequest.getExternalVoterReference(),
                Set.copyOf(voterRegistrationRequest.getEligibleElectionIds()),
                RegistrationStatus.ACTIVE,
                actorId(),
                sourceIp()));
        return ResponseEntity.created(URI.create("/voters/" + voter.getId())).body(toMaskedResponse(voter));
    }

    @Override
    public ResponseEntity<VoterMaskedResponse> getVoter(UUID voterId) {
        ensureCanAccessVoter(voterId);
        VoterRecord voter = service().getVoter(voterId)
                .orElseThrow(() -> new VoterNotFoundException("Voter not found: " + voterId));
        return ResponseEntity.ok(toMaskedResponse(voter));
    }

    @Override
    public ResponseEntity<VoterEligibilityResponse> checkVoterEligibility(UUID voterId, UUID electionId) {
        ensureCanAccessVoter(voterId);
        VoterEligibilityResult result = service().checkEligibility(new VoterRollService.CheckVoterEligibilityCommand(
                voterId,
                electionId,
                20,
                true,
                ElectionType.PRESIDENTIAL));
        return ResponseEntity.ok(new VoterEligibilityResponse(voterId, electionId, result.eligible()).reason(result.reason()));
    }

    private VoterMaskedResponse toMaskedResponse(VoterRecord voter) {
        return new VoterMaskedResponse(
                voter.getId(),
                maskedExternalReference(voter),
                VoterMaskedResponse.RegistrationStatusEnum.fromValue(voter.getRegistrationStatus().name()));
    }

    private String maskedExternalReference(VoterRecord voter) {
        PiiEncryptionService service = encryptionService.getIfAvailable();
        if (service == null) {
            return "****";
        }
        String externalReference = voter.decryptExternalVoterId(service);
        if (externalReference.length() <= 4) {
            return "****";
        }
        return "*".repeat(externalReference.length() - 4) + externalReference.substring(externalReference.length() - 4);
    }

    private VoterRollService service() {
        VoterRollService service = voterRollService.getIfAvailable();
        if (service == null) {
            throw new VoterServiceUnavailableException("Voter roll service is unavailable");
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
        throw new AccessDeniedException("Voter access denied");
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

    public static final class VoterNotFoundException extends RuntimeException {
        public VoterNotFoundException(String message) {
            super(message);
        }
    }

    public static final class VoterServiceUnavailableException extends RuntimeException {
        public VoterServiceUnavailableException(String message) {
            super(message);
        }
    }
}
