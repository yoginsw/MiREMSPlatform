package io.mirems.core.bpmn.voter;

import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.voting.RegistrationStatus;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class VoterEligibilityDecisionService {
    public static final String DECISION_ID = "VoterEligibilityCheck";
    public static final String DMN_RESOURCE = "decisions/VoterEligibilityCheck.dmn";

    public VoterEligibilityResult evaluate(VoterEligibilityRequest request) {
        VoterEligibilityRequest input = Objects.requireNonNull(request, "request is required");
        if (input.voterAge() < 18) {
            return new VoterEligibilityResult(false, "voter age must be at least 18");
        }
        if (input.registrationStatus() != RegistrationStatus.ACTIVE) {
            return new VoterEligibilityResult(false, "voter registration status must be ACTIVE");
        }
        if (!input.residencyVerified()) {
            return new VoterEligibilityResult(false, "voter residency must be verified");
        }
        if (input.electionType() == ElectionType.LOCAL && input.voterAge() < 19) {
            return new VoterEligibilityResult(false, "LOCAL elections require voter age at least 19 by core policy");
        }
        return new VoterEligibilityResult(true, "eligible");
    }
}
