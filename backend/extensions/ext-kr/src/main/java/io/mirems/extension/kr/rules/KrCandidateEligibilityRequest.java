package io.mirems.extension.kr.rules;

import io.mirems.extension.kr.KrElectionType;

public record KrCandidateEligibilityRequest(
        int candidateAge,
        KrCitizenshipStatus citizenship,
        boolean hasDisqualifyingCriminalRecord,
        KrElectionType electionType) {}
