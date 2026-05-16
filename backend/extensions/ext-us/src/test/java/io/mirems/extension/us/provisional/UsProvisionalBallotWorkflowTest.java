package io.mirems.extension.us.provisional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mirems.extension.us.rules.UsCitizenshipStatus;
import io.mirems.extension.us.rules.UsElectionType;
import io.mirems.extension.us.rules.UsIdVerificationStatus;
import io.mirems.extension.us.rules.UsVoterEligibilityRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsProvisionalBallotWorkflowTest {
    private static final UUID VOTER_ID = UUID.fromString("018f4c00-0000-7000-8000-000000000101");
    private static final UUID ELECTION_ID = UUID.fromString("018f4c00-0000-7000-8000-000000000102");
    private static final UUID BALLOT_ID = UUID.fromString("018f4c00-0000-7000-8000-000000000103");
    private static final OffsetDateTime CAST_AT = OffsetDateTime.of(2028, 11, 7, 10, 0, 0, 0, ZoneOffset.UTC);

    private final UsProvisionalBallotWorkflowService service = new UsProvisionalBallotWorkflowService();

    @Test
    void createsProvisionalBallotWhenHavaIdIsUnverifiedAndKeepsAuditReasonGeneric() {
        UsVoterEligibilityRequest request = new UsVoterEligibilityRequest(
                UsCitizenshipStatus.CITIZEN,
                LocalDate.of(1988, 1, 1),
                LocalDate.of(2028, 11, 7),
                LocalDate.of(2028, 11, 7),
                UsElectionType.GENERAL_ELECTION,
                "CA",
                UsIdVerificationStatus.UNVERIFIED_HAVA_ID,
                true);

        UsProvisionalBallot ballot = service.createFromEligibility(VOTER_ID, ELECTION_ID, BALLOT_ID, request, CAST_AT);

        assertThat(ballot.status()).isEqualTo(UsProvisionalBallotStatus.PENDING_REVIEW);
        assertThat(ballot.reasonCode()).isEqualTo("HAVA_ID_UNVERIFIED");
        assertThat(ballot.auditTrail()).containsExactly("CREATED:HAVA_ID_UNVERIFIED");
        assertThat(ballot.auditSummary()).isEqualTo("provisionalBallot=" + ballot.id() + "; status=PENDING_REVIEW; reason=HAVA_ID_UNVERIFIED");
    }

    @Test
    void resolvesProvisionalBallotWithDistinctDecisionAuditAndRejectsDoubleResolution() {
        UsProvisionalBallot ballot = service.createManual(
                VOTER_ID,
                ELECTION_ID,
                BALLOT_ID,
                "PRECINCT_MISMATCH",
                CAST_AT);

        UsProvisionalBallot accepted = service.resolve(ballot, true, "county-board-1", CAST_AT.plusDays(2));

        assertThat(accepted.status()).isEqualTo(UsProvisionalBallotStatus.ACCEPTED);
        assertThat(accepted.resolvedBy()).contains("county-board-1");
        assertThat(accepted.auditTrail()).containsExactly("CREATED:PRECINCT_MISMATCH", "RESOLVED:ACCEPTED");
        assertThatThrownBy(() -> service.resolve(accepted, false, "county-board-2", CAST_AT.plusDays(3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already resolved");
    }

    @Test
    void rejectsNonProvisionalEligibilityAndBlankReasonCodes() {
        UsVoterEligibilityRequest eligibleRequest = new UsVoterEligibilityRequest(
                UsCitizenshipStatus.CITIZEN,
                LocalDate.of(1988, 1, 1),
                LocalDate.of(2028, 11, 7),
                LocalDate.of(2028, 11, 7),
                UsElectionType.GENERAL_ELECTION,
                "CA",
                UsIdVerificationStatus.VERIFIED,
                true);

        assertThatThrownBy(() -> service.createFromEligibility(VOTER_ID, ELECTION_ID, BALLOT_ID, eligibleRequest, CAST_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not require a provisional ballot");
        assertThatThrownBy(() -> service.createManual(VOTER_ID, ELECTION_ID, BALLOT_ID, " ", CAST_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reasonCode is required");
    }
}
