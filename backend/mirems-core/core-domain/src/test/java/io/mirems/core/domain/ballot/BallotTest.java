package io.mirems.core.domain.ballot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BallotTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4b7e-77d3-7c22-b7ec-6e89229b7d43");
    private static final UUID BALLOT_ID = UUID.fromString("018f4b7e-a106-7f55-da1f-91a2552e0a76");
    private static final UUID STYLE_ID = UUID.fromString("018f4b7e-b217-7066-eb20-a2b3663f1b87");
    private static final UUID CONTEST_ID = UUID.fromString("018f4b7e-88e4-7d33-b8fd-7f90330c8e54");

    @Test
    void createBallotInitializesDraftVersionAndInactiveState() {
        Election election = election();

        Ballot ballot = Ballot.create(BALLOT_ID, election);

        assertEquals(BALLOT_ID, ballot.getId());
        assertSame(election, ballot.getElection());
        assertEquals(1, ballot.getBallotVersion());
        assertFalse(ballot.isActive());
        assertTrue(ballot.getBallotContests().isEmpty());
        assertTrue(ballot.getBallotStyles().isEmpty());
    }

    @Test
    void ballotRejectsInvalidRequiredFields() {
        assertThrows(NullPointerException.class, () -> Ballot.create(null, election()));
        assertThrows(NullPointerException.class, () -> Ballot.create(BALLOT_ID, null));
    }

    @Test
    void addContestCreatesOrderedBallotContestAndIncrementsVersion() {
        Ballot ballot = Ballot.create(BALLOT_ID, election());
        Contest contest = contest();

        BallotContest ballotContest = ballot.addContest(contest, 1, "Mayor selection");

        assertEquals(2, ballot.getBallotVersion());
        assertSame(ballot, ballotContest.getBallot());
        assertSame(contest, ballotContest.getContest());
        assertEquals(1, ballotContest.getDisplayOrder());
        assertEquals("Mayor selection", ballotContest.getPresentationTitle());
        assertEquals(1, ballot.getBallotContests().size());
        assertSame(ballotContest, ballot.getBallotContests().getFirst());
    }

    @Test
    void ballotContestRejectsInvalidFields() {
        Ballot ballot = Ballot.create(BALLOT_ID, election());

        assertThrows(NullPointerException.class, () -> ballot.addContest(null, 1, "Mayor"));
        assertThrows(BallotValidationException.class, () -> ballot.addContest(contest(), 0, "Mayor"));
        assertThrows(IllegalArgumentException.class, () -> ballot.addContest(contest(), 1, " "));
    }

    @Test
    void activateRequiresAtLeastOneContestAndIncrementsVersion() {
        Ballot emptyBallot = Ballot.create(BALLOT_ID, election());
        BallotValidationException exception = assertThrows(BallotValidationException.class, emptyBallot::activate);
        assertEquals("MIR-BALLOT-VALIDATION-001", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("at least one Contest"));
        assertFalse(emptyBallot.isActive());
        assertEquals(1, emptyBallot.getBallotVersion());

        Ballot ballot = Ballot.create(BALLOT_ID, election());
        ballot.addContest(contest(), 1, "Mayor selection");
        ballot.activate();

        assertTrue(ballot.isActive());
        assertEquals(3, ballot.getBallotVersion());
    }

    @Test
    void deactivateChangesActiveStateAndIncrementsVersionOnlyWhenStateChanges() {
        Ballot ballot = Ballot.create(BALLOT_ID, election());
        ballot.addContest(contest(), 1, "Mayor selection");
        ballot.activate();

        ballot.deactivate();
        ballot.deactivate();

        assertFalse(ballot.isActive());
        assertEquals(4, ballot.getBallotVersion());
    }

    @Test
    void addBallotStyleInitializesFieldsAndIncrementsVersion() {
        Ballot ballot = Ballot.create(BALLOT_ID, election());

        BallotStyle style = ballot.addStyle(
                STYLE_ID,
                "SEOUL-01-KO-A11Y",
                "Seoul District 1",
                "ko",
                Set.of(AccessibilityFeature.AUDIO, AccessibilityFeature.HIGH_CONTRAST));

        assertEquals(2, ballot.getBallotVersion());
        assertEquals(STYLE_ID, style.getId());
        assertSame(ballot, style.getBallot());
        assertEquals("SEOUL-01-KO-A11Y", style.getStyleCode());
        assertEquals("Seoul District 1", style.getDistrict());
        assertEquals("ko", style.getLanguage());
        assertEquals(Set.of(AccessibilityFeature.AUDIO, AccessibilityFeature.HIGH_CONTRAST), style.getAccessibilityFeatures());
        assertEquals(1, ballot.getBallotStyles().size());
        assertSame(style, ballot.getBallotStyles().getFirst());
    }

    @Test
    void ballotStyleRejectsInvalidRequiredFields() {
        Ballot ballot = Ballot.create(BALLOT_ID, election());

        assertThrows(NullPointerException.class, () -> ballot.addStyle(
                null, "SEOUL-01-KO", "Seoul District 1", "ko", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> ballot.addStyle(
                STYLE_ID, " ", "Seoul District 1", "ko", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> ballot.addStyle(
                STYLE_ID, "SEOUL-01-KO", " ", "ko", Set.of()));
        assertThrows(NullPointerException.class, () -> ballot.addStyle(
                STYLE_ID, "SEOUL-01-KO", "Seoul District 1", "ko", null));
    }

    @Test
    void ballotStyleRequiresValidIso639DashOneLanguage() {
        Ballot ballot = Ballot.create(BALLOT_ID, election());

        BallotValidationException exception = assertThrows(BallotValidationException.class, () -> ballot.addStyle(
                STYLE_ID,
                "SEOUL-01-KR",
                "Seoul District 1",
                "zz",
                Set.of(AccessibilityFeature.LARGE_TEXT)));

        assertEquals("MIR-BALLOT-VALIDATION-001", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("ISO 639-1"));
    }

    @Test
    void exposedCollectionsAreImmutableSnapshots() {
        Ballot ballot = Ballot.create(BALLOT_ID, election());
        ballot.addContest(contest(), 1, "Mayor selection");
        ballot.addStyle(STYLE_ID, "SEOUL-01-KO", "Seoul District 1", "ko", Set.of(AccessibilityFeature.LARGE_TEXT));

        assertThrows(UnsupportedOperationException.class, () -> ballot.getBallotContests().add(
                ballot.getBallotContests().getFirst()));
        assertThrows(UnsupportedOperationException.class, () -> ballot.getBallotStyles().clear());
        assertThrows(UnsupportedOperationException.class, () -> ballot.getBallotStyles().getFirst()
                .getAccessibilityFeatures().add(AccessibilityFeature.AUDIO));
    }

    private static Contest contest() {
        return Contest.create(
                CONTEST_ID,
                election(),
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1);
    }

    private static Election election() {
        Election election = Election.create(
                ELECTION_ID,
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                LocalDate.of(2026, 6, 3),
                "KR",
                "ext-kr");
        election.pullDomainEvents();
        return election;
    }
}
