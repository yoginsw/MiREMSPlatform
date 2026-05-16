package io.mirems.extension.kr.rules;

import io.mirems.extension.kr.KrElectionType;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class KrCandidateEligibilityDecisionService {
    public static final String DECISION_ID = "KrCandidateEligibility";
    public static final String DMN_RESOURCE = "decisions/kr/KrCandidateEligibility.dmn";

    public KrCandidateEligibilityResult evaluate(KrCandidateEligibilityRequest request) {
        KrCandidateEligibilityRequest input = Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(input.citizenship(), "citizenship is required");
        Objects.requireNonNull(input.electionType(), "electionType is required");

        if (input.citizenship() != KrCitizenshipStatus.CITIZEN) {
            return new KrCandidateEligibilityResult(false, "candidate must be a Korean citizen");
        }
        if (input.hasDisqualifyingCriminalRecord()) {
            return new KrCandidateEligibilityResult(false, "candidate has a disqualifying criminal record");
        }
        if (input.electionType() == KrElectionType.PRESIDENTIAL_ELECTION && input.candidateAge() < 40) {
            return new KrCandidateEligibilityResult(false, "presidential candidates must be at least 40");
        }
        if (input.electionType() != KrElectionType.PRESIDENTIAL_ELECTION && input.candidateAge() < 18) {
            return new KrCandidateEligibilityResult(false, "non-presidential candidates must be at least 18");
        }
        return new KrCandidateEligibilityResult(true, "eligible Korean citizen candidate");
    }
}
