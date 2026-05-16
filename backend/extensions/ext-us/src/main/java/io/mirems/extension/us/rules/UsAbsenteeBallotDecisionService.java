package io.mirems.extension.us.rules;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class UsAbsenteeBallotDecisionService {
    public static final String DECISION_ID = "UsAbsenteeBallot";
    public static final String DMN_RESOURCE = "decisions/us/UsAbsenteeBallot.dmn";

    public UsAbsenteeBallotResult evaluate(UsAbsenteeBallotRequest request) {
        UsAbsenteeBallotRequest input = Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(input.voterCategory(), "voterCategory is required");
        Objects.requireNonNull(input.ballotRequestDate(), "ballotRequestDate is required");
        Objects.requireNonNull(input.electionDay(), "electionDay is required");
        normalizeStateCode(input.stateCode());

        if (input.voterCategory() == UsAbsenteeVoterCategory.NOT_ABSENTEE_ELIGIBLE) {
            return new UsAbsenteeBallotResult(false, false, "absentee category is not eligible");
        }
        if (isUocava(input.voterCategory())
                && input.blankBallotNotReceived()
                && daysBeforeElection(input) >= 0
                && daysBeforeElection(input) <= 45) {
            return new UsAbsenteeBallotResult(
                    true, true, "UOCAVA late request permits federal write-in absentee ballot");
        }
        if (input.voterCategory() == UsAbsenteeVoterCategory.MILITARY) {
            return new UsAbsenteeBallotResult(true, false, "eligible UOCAVA military voter");
        }
        if (input.voterCategory() == UsAbsenteeVoterCategory.OVERSEAS_CITIZEN) {
            return new UsAbsenteeBallotResult(true, false, "eligible UOCAVA overseas citizen voter");
        }
        return new UsAbsenteeBallotResult(true, false, "eligible state absentee voter");
    }

    private static boolean isUocava(UsAbsenteeVoterCategory category) {
        return category == UsAbsenteeVoterCategory.MILITARY || category == UsAbsenteeVoterCategory.OVERSEAS_CITIZEN;
    }

    private static long daysBeforeElection(UsAbsenteeBallotRequest input) {
        return ChronoUnit.DAYS.between(input.ballotRequestDate(), input.electionDay());
    }

    private static String normalizeStateCode(String stateCode) {
        String normalized = Objects.requireNonNull(stateCode, "stateCode is required").trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("stateCode must be a two-letter USPS code");
        }
        return normalized;
    }
}
