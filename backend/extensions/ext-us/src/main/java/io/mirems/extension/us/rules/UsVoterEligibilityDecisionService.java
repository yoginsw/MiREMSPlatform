package io.mirems.extension.us.rules;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UsVoterEligibilityDecisionService {
    public static final String DECISION_ID = "UsVoterEligibility";
    public static final String DMN_RESOURCE = "decisions/us/UsVoterEligibility.dmn";

    private static final Set<String> PRIMARY_AGE_BY_GENERAL_STATES = Set.of("MD");

    public UsVoterEligibilityResult evaluate(UsVoterEligibilityRequest request) {
        UsVoterEligibilityRequest input = Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(input.citizenshipStatus(), "citizenshipStatus is required");
        Objects.requireNonNull(input.birthDate(), "birthDate is required");
        Objects.requireNonNull(input.electionDay(), "electionDay is required");
        Objects.requireNonNull(input.generalElectionDay(), "generalElectionDay is required");
        Objects.requireNonNull(input.electionType(), "electionType is required");
        Objects.requireNonNull(input.idVerificationStatus(), "idVerificationStatus is required");
        String stateCode = normalizeStateCode(input.stateCode());

        if (input.citizenshipStatus() != UsCitizenshipStatus.CITIZEN) {
            return new UsVoterEligibilityResult(false, false, "US federal elections require citizenship");
        }
        if (!input.registered()) {
            return new UsVoterEligibilityResult(false, false, "voter registration is required");
        }
        if (!meetsAgeRule(input, stateCode)) {
            String reason = input.electionType() == UsElectionType.PRIMARY_ELECTION
                    ? "voter must be 18 on primary election day"
                    : "voter must be 18 on election day";
            return new UsVoterEligibilityResult(false, false, reason);
        }
        if (input.idVerificationStatus() == UsIdVerificationStatus.UNVERIFIED_HAVA_ID) {
            return new UsVoterEligibilityResult(true, true, "HAVA ID verification requires provisional ballot");
        }
        if (input.electionType() == UsElectionType.PRIMARY_ELECTION
                && PRIMARY_AGE_BY_GENERAL_STATES.contains(stateCode)
                && ageOn(input.birthDate(), input.electionDay()) < 18) {
            return new UsVoterEligibilityResult(
                    true, false, "eligible primary voter turning 18 by general election");
        }
        return new UsVoterEligibilityResult(true, false, "eligible verified voter");
    }

    private static boolean meetsAgeRule(UsVoterEligibilityRequest input, String stateCode) {
        if (input.electionType() == UsElectionType.PRIMARY_ELECTION
                && PRIMARY_AGE_BY_GENERAL_STATES.contains(stateCode)) {
            return ageOn(input.birthDate(), input.generalElectionDay()) >= 18;
        }
        return ageOn(input.birthDate(), input.electionDay()) >= 18;
    }

    private static int ageOn(LocalDate birthDate, LocalDate date) {
        return Period.between(birthDate, date).getYears();
    }

    private static String normalizeStateCode(String stateCode) {
        String normalized = Objects.requireNonNull(stateCode, "stateCode is required").trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("stateCode must be a two-letter USPS code");
        }
        return normalized;
    }
}
