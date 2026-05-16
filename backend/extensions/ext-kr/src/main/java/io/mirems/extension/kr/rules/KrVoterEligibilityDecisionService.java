package io.mirems.extension.kr.rules;

import io.mirems.extension.kr.KrElectionType;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class KrVoterEligibilityDecisionService {
    public static final String DECISION_ID = "KrVoterEligibility";
    public static final String DMN_RESOURCE = "decisions/kr/KrVoterEligibility.dmn";

    public KrVoterEligibilityResult evaluate(KrVoterEligibilityRequest request) {
        KrVoterEligibilityRequest input = Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(input.citizenship(), "citizenship is required");
        Objects.requireNonNull(input.electionType(), "electionType is required");

        if (input.voterAge() < 18) {
            return new KrVoterEligibilityResult(false, "voter age must be at least 18");
        }
        if (input.citizenship() == KrCitizenshipStatus.CITIZEN) {
            return new KrVoterEligibilityResult(true, "eligible Korean citizen voter");
        }
        if (isLocalSuffrageElection(input.electionType())
                && input.citizenship() == KrCitizenshipStatus.FOREIGN_PERMANENT_RESIDENT
                && input.permanentResident()) {
            return new KrVoterEligibilityResult(true, "eligible permanent resident local voter");
        }
        if (!isLocalSuffrageElection(input.electionType())) {
            return new KrVoterEligibilityResult(false, "national elections require Korean citizenship");
        }
        return new KrVoterEligibilityResult(false, "local elections require Korean citizenship or permanent resident status");
    }

    private boolean isLocalSuffrageElection(KrElectionType electionType) {
        return electionType == KrElectionType.LOCAL_ELECTION
                || electionType == KrElectionType.SUPERINTENDENT_ELECTION;
    }
}
