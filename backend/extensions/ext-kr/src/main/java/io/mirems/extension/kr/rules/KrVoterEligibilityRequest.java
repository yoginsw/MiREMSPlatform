package io.mirems.extension.kr.rules;

import io.mirems.extension.kr.KrElectionType;

public record KrVoterEligibilityRequest(
        int voterAge,
        KrCitizenshipStatus citizenship,
        boolean permanentResident,
        KrElectionType electionType) {}
