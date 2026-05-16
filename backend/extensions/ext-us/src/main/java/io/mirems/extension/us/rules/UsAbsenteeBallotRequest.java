package io.mirems.extension.us.rules;

import java.time.LocalDate;

public record UsAbsenteeBallotRequest(
        UsAbsenteeVoterCategory voterCategory,
        LocalDate ballotRequestDate,
        LocalDate electionDay,
        String stateCode,
        boolean blankBallotNotReceived) {}
