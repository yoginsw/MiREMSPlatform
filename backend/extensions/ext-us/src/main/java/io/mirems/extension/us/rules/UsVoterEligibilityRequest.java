package io.mirems.extension.us.rules;

import java.time.LocalDate;

public record UsVoterEligibilityRequest(
        UsCitizenshipStatus citizenshipStatus,
        LocalDate birthDate,
        LocalDate electionDay,
        LocalDate generalElectionDay,
        UsElectionType electionType,
        String stateCode,
        UsIdVerificationStatus idVerificationStatus,
        boolean registered) {}
