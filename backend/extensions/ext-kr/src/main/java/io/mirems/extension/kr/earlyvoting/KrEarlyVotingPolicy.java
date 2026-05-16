package io.mirems.extension.kr.earlyvoting;

import io.mirems.core.domain.voting.VotingMethod;
import io.mirems.core.domain.voting.VotingSessionOpeningContext;
import io.mirems.core.domain.voting.VotingSessionOpeningPolicy;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class KrEarlyVotingPolicy implements VotingSessionOpeningPolicy {
    private static final int EARLY_VOTING_START_DAYS_BEFORE_ELECTION = 5;
    private static final int EARLY_VOTING_END_DAYS_BEFORE_ELECTION = 4;

    @Override
    public void validate(VotingSessionOpeningContext context) {
        Objects.requireNonNull(context, "context is required");
        if (!"KR".equalsIgnoreCase(context.election().getCountryCode())) {
            return;
        }
        validate(
                context.votingMethod(),
                context.election().getScheduledDate(),
                context.openedAt().toLocalDate(),
                context.homeDistrictCode(),
                context.pollingStationDistrictCode());
    }

    public boolean isVotingDateAllowed(VotingMethod votingMethod, LocalDate electionDay, LocalDate votingDate) {
        VotingMethod method = Objects.requireNonNull(votingMethod, "votingMethod is required");
        LocalDate election = Objects.requireNonNull(electionDay, "electionDay is required");
        LocalDate date = Objects.requireNonNull(votingDate, "votingDate is required");
        return switch (method) {
            case EARLY_VOTING -> !date.isBefore(election.minusDays(EARLY_VOTING_START_DAYS_BEFORE_ELECTION))
                    && !date.isAfter(election.minusDays(EARLY_VOTING_END_DAYS_BEFORE_ELECTION));
            case ELECTION_DAY -> date.equals(election);
            case ABSENTEE -> true;
        };
    }

    public boolean isPollingStationAllowed(
            VotingMethod votingMethod,
            String homeDistrictCode,
            String pollingStationDistrictCode) {
        VotingMethod method = Objects.requireNonNull(votingMethod, "votingMethod is required");
        String homeDistrict = requireText(homeDistrictCode, "homeDistrictCode");
        String pollingDistrict = requireText(pollingStationDistrictCode, "pollingStationDistrictCode");
        return switch (method) {
            case EARLY_VOTING -> true;
            case ELECTION_DAY, ABSENTEE -> homeDistrict.equals(pollingDistrict);
        };
    }

    public void validate(
            VotingMethod votingMethod,
            LocalDate electionDay,
            LocalDate votingDate,
            String homeDistrictCode,
            String pollingStationDistrictCode) {
        if (!isVotingDateAllowed(votingMethod, electionDay, votingDate)) {
            throw new KrEarlyVotingValidationException(
                    "KR early voting is allowed only from D-5 to D-4 before election day");
        }
        if (!isPollingStationAllowed(votingMethod, homeDistrictCode, pollingStationDistrictCode)) {
            throw new KrEarlyVotingValidationException(
                    "Election-day voting must use the voter's home district polling station");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
