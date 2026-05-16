package io.mirems.extension.us.absentee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mirems.extension.us.rules.UsAbsenteeBallotRequest;
import io.mirems.extension.us.rules.UsAbsenteeVoterCategory;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsAbsenteeBallotTrackingServiceTest {
    private static final UUID VOTER_ID = UUID.fromString("018f4c00-0000-7000-8000-000000000201");
    private static final UUID ELECTION_ID = UUID.fromString("018f4c00-0000-7000-8000-000000000202");
    private static final LocalDate ELECTION_DAY = LocalDate.of(2028, 11, 7);
    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.of(2028, 9, 25, 12, 0, 0, 0, ZoneOffset.UTC);

    private final UsAbsenteeBallotTrackingService service = new UsAbsenteeBallotTrackingService();

    @Test
    void tracksUocavaAbsenteeBallotFromRequestThroughAcceptance() {
        UsAbsenteeBallotRequest request = new UsAbsenteeBallotRequest(
                UsAbsenteeVoterCategory.MILITARY,
                ELECTION_DAY.minusDays(43),
                ELECTION_DAY,
                "CA",
                false);

        UsAbsenteeBallotRecord record = service.requestBallot(VOTER_ID, ELECTION_ID, request, REQUESTED_AT);
        UsAbsenteeBallotRecord sent = service.markSent(record, REQUESTED_AT.plusDays(1), "mail-plus-email");
        UsAbsenteeBallotRecord returned = service.markReturned(sent, REQUESTED_AT.plusDays(21));
        UsAbsenteeBallotRecord accepted = service.adjudicate(returned, true, "signature verified", REQUESTED_AT.plusDays(22));

        assertThat(record.status()).isEqualTo(UsAbsenteeBallotStatus.REQUESTED);
        assertThat(accepted.status()).isEqualTo(UsAbsenteeBallotStatus.ACCEPTED);
        assertThat(accepted.uocava()).isTrue();
        assertThat(accepted.auditTrail()).containsExactly(
                "REQUESTED:MILITARY",
                "SENT:mail-plus-email",
                "RETURNED",
                "ADJUDICATED:ACCEPTED");
        assertThat(accepted.auditSummary()).contains("status=ACCEPTED").doesNotContain("signature verified");
    }

    @Test
    void allowsFwabFallbackOnlyForUocavaVoterInsideFortyFiveDayWindow() {
        UsAbsenteeBallotRequest fwabRequest = new UsAbsenteeBallotRequest(
                UsAbsenteeVoterCategory.OVERSEAS_CITIZEN,
                ELECTION_DAY.minusDays(10),
                ELECTION_DAY,
                "MD",
                true);

        UsAbsenteeBallotRecord fwab = service.requestBallot(VOTER_ID, ELECTION_ID, fwabRequest, REQUESTED_AT);

        assertThat(fwab.status()).isEqualTo(UsAbsenteeBallotStatus.FWAB_ALLOWED);
        assertThat(fwab.federalWriteInAbsenteeBallotAllowed()).isTrue();
        assertThatThrownBy(() -> service.markSent(fwab, REQUESTED_AT.plusDays(1), "email"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FWAB fallback records cannot be sent");
    }

    @Test
    void rejectsIneligibleDomesticAndOutOfOrderTransitions() {
        UsAbsenteeBallotRequest ineligible = new UsAbsenteeBallotRequest(
                UsAbsenteeVoterCategory.NOT_ABSENTEE_ELIGIBLE,
                ELECTION_DAY.minusDays(30),
                ELECTION_DAY,
                "CA",
                false);

        assertThatThrownBy(() -> service.requestBallot(VOTER_ID, ELECTION_ID, ineligible, REQUESTED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not absentee eligible");

        UsAbsenteeBallotRecord requested = service.requestBallot(
                VOTER_ID,
                ELECTION_ID,
                new UsAbsenteeBallotRequest(UsAbsenteeVoterCategory.DOMESTIC_NO_EXCUSE, ELECTION_DAY.minusDays(20), ELECTION_DAY, "CA", false),
                REQUESTED_AT);
        assertThatThrownBy(() -> service.markReturned(requested, REQUESTED_AT.plusDays(3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be SENT before RETURNED");
    }
}
