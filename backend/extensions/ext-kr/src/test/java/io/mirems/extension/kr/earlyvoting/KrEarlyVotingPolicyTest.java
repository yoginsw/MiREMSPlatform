package io.mirems.extension.kr.earlyvoting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.voting.VotingMethod;
import io.mirems.core.domain.voting.VotingSessionOpeningContext;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KrEarlyVotingPolicyTest {
    private final KrEarlyVotingPolicy policy = new KrEarlyVotingPolicy();
    private static final LocalDate ELECTION_DAY = LocalDate.of(2026, 6, 3);

    @Test
    void earlyVotingIsAllowedOnlyFromFiveDaysToFourDaysBeforeElectionDay() {
        assertThat(policy.isVotingDateAllowed(VotingMethod.EARLY_VOTING, ELECTION_DAY, LocalDate.of(2026, 5, 29)))
                .isTrue();
        assertThat(policy.isVotingDateAllowed(VotingMethod.EARLY_VOTING, ELECTION_DAY, LocalDate.of(2026, 5, 30)))
                .isTrue();

        assertThat(policy.isVotingDateAllowed(VotingMethod.EARLY_VOTING, ELECTION_DAY, LocalDate.of(2026, 5, 28)))
                .isFalse();
        assertThat(policy.isVotingDateAllowed(VotingMethod.EARLY_VOTING, ELECTION_DAY, LocalDate.of(2026, 5, 31)))
                .isFalse();
        assertThat(policy.isVotingDateAllowed(VotingMethod.EARLY_VOTING, ELECTION_DAY, ELECTION_DAY))
                .isFalse();
    }

    @Test
    void electionDayVotingIsAllowedOnlyOnElectionDay() {
        assertThat(policy.isVotingDateAllowed(VotingMethod.ELECTION_DAY, ELECTION_DAY, ELECTION_DAY)).isTrue();
        assertThat(policy.isVotingDateAllowed(VotingMethod.ELECTION_DAY, ELECTION_DAY, ELECTION_DAY.minusDays(1)))
                .isFalse();
    }

    @Test
    void earlyVotingAllowsCrossDistrictPollingStationsButElectionDayRequiresHomeDistrict() {
        assertThat(policy.isPollingStationAllowed(
                        VotingMethod.EARLY_VOTING, "SEOUL-JONGNO", "BUSAN-HAEUNDAE"))
                .isTrue();
        assertThat(policy.isPollingStationAllowed(
                        VotingMethod.ELECTION_DAY, "SEOUL-JONGNO", "SEOUL-JONGNO"))
                .isTrue();
        assertThat(policy.isPollingStationAllowed(
                        VotingMethod.ELECTION_DAY, "SEOUL-JONGNO", "BUSAN-HAEUNDAE"))
                .isFalse();
    }

    @Test
    void validateRejectsOutOfPeriodOrDisallowedPollingStation() {
        assertThatThrownBy(() -> policy.validate(
                        VotingMethod.EARLY_VOTING,
                        ELECTION_DAY,
                        ELECTION_DAY.minusDays(6),
                        "SEOUL-JONGNO",
                        "BUSAN-HAEUNDAE"))
                .isInstanceOf(KrEarlyVotingValidationException.class)
                .hasMessageContaining("D-5 to D-4");

        assertThatThrownBy(() -> policy.validate(
                        VotingMethod.ELECTION_DAY,
                        ELECTION_DAY,
                        ELECTION_DAY,
                        "SEOUL-JONGNO",
                        "BUSAN-HAEUNDAE"))
                .isInstanceOf(KrEarlyVotingValidationException.class)
                .hasMessageContaining("home district");
    }

    @Test
    void rejectsMissingRequiredInputs() {
        assertThatThrownBy(() -> policy.isVotingDateAllowed(null, ELECTION_DAY, ELECTION_DAY))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> policy.isPollingStationAllowed(VotingMethod.EARLY_VOTING, " ", "SEOUL-JONGNO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("homeDistrictCode");
    }

    @Test
    void votingSessionOpeningPolicyEnforcesKrElectionContext() {
        Election election = krElection(ELECTION_DAY);

        policy.validate(new VotingSessionOpeningContext(
                null,
                election,
                null,
                VotingMethod.EARLY_VOTING,
                OffsetDateTime.parse("2026-05-29T09:00:00Z"),
                "SEOUL-JONGNO",
                "BUSAN-HAEUNDAE"));

        assertThatThrownBy(() -> policy.validate(new VotingSessionOpeningContext(
                        null,
                        election,
                        null,
                        VotingMethod.EARLY_VOTING,
                        OffsetDateTime.parse("2026-05-31T09:00:00Z"),
                        "SEOUL-JONGNO",
                        "BUSAN-HAEUNDAE")))
                .isInstanceOf(KrEarlyVotingValidationException.class)
                .hasMessageContaining("D-5 to D-4");
    }

    @Test
    void votingSessionOpeningPolicySkipsNonKrElections() {
        Election election = Election.create(
                UUID.fromString("018f4b81-8888-7888-8888-888888888888"),
                "US General Election",
                ElectionType.PARLIAMENTARY,
                "US-CA",
                ELECTION_DAY,
                "US",
                "ext-us");

        policy.validate(new VotingSessionOpeningContext(
                null,
                election,
                null,
                VotingMethod.EARLY_VOTING,
                OffsetDateTime.parse("2026-05-31T09:00:00Z"),
                null,
                null));
    }

    private static Election krElection(LocalDate electionDay) {
        return Election.create(
                UUID.fromString("018f4b81-9999-7999-8999-999999999999"),
                "KR National Assembly Election",
                ElectionType.PARLIAMENTARY,
                "KR-SEOUL",
                electionDay,
                "KR",
                "ext-kr");
    }
}
