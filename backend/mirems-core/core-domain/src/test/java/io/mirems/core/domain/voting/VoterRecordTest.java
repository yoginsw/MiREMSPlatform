package io.mirems.core.domain.voting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.exception.DomainException;

import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.voting.encryption.EncryptedStringJpaConverter;
import io.mirems.core.domain.voting.encryption.PiiEncryptionException;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VoterRecordTest {
    private static final UUID VOTER_ID = UUID.fromString("018f4b7e-c328-7177-fc31-b3c4774f2c98");
    private static final UUID SESSION_ID = UUID.fromString("018f4b7e-d439-7288-ad42-c4d588503da9");
    private static final UUID ELECTION_ID = UUID.fromString("018f4b7e-77d3-7c22-b7ec-6e89229b7d43");
    private static final UUID BALLOT_ID = UUID.fromString("018f4b7e-a106-7f55-da1f-91a2552e0a76");
    private static final UUID STYLE_ID = UUID.fromString("018f4b7e-b217-7066-eb20-a2b3663f1b87");
    private static final UUID CONTEST_ID = UUID.fromString("018f4b7e-88e4-7d33-b8fd-7f90330c8e54");
    private static final String EXTERNAL_VOTER_ID = "KR-SEOUL-000001";

    @Test
    void piiEncryptionServiceEncryptsWithAes256AndRoundTrips() {
        PiiEncryptionService service = encryptionService();

        String ciphertext = service.encrypt(EXTERNAL_VOTER_ID);

        assertNotEquals(EXTERNAL_VOTER_ID, ciphertext);
        assertFalse(ciphertext.contains(EXTERNAL_VOTER_ID));
        assertEquals(EXTERNAL_VOTER_ID, service.decrypt(ciphertext));
        assertNotEquals(ciphertext, service.encrypt(EXTERNAL_VOTER_ID));
    }

    @Test
    void piiEncryptionServiceRejectsInvalidInputsAndTamperedPayloads() {
        assertThrows(IllegalArgumentException.class, () -> new PiiEncryptionService(new byte[31]));
        PiiEncryptionService service = encryptionService();

        assertThrows(IllegalArgumentException.class, () -> service.encrypt(" "));
        assertThrows(IllegalArgumentException.class, () -> service.decrypt(" "));
        assertThrows(PiiEncryptionException.class, () -> service.decrypt("AA=="));

        String ciphertext = service.encrypt(EXTERNAL_VOTER_ID);
        char replacement = ciphertext.charAt(ciphertext.length() - 1) == 'A' ? 'B' : 'A';
        String tampered = ciphertext.substring(0, ciphertext.length() - 1) + replacement;
        assertThrows(PiiEncryptionException.class, () -> service.decrypt(tampered));
    }

    @Test
    void encryptedJpaConverterRoundTripsNullAndNonNullValues() {
        EncryptedStringJpaConverter converter = new EncryptedStringJpaConverter(encryptionService());

        String databaseValue = converter.convertToDatabaseColumn(EXTERNAL_VOTER_ID);

        assertNotEquals(EXTERNAL_VOTER_ID, databaseValue);
        assertEquals(EXTERNAL_VOTER_ID, converter.convertToEntityAttribute(databaseValue));
        assertEquals(null, converter.convertToDatabaseColumn(null));
        assertEquals(null, converter.convertToEntityAttribute(null));
        assertThrows(NullPointerException.class, () -> new EncryptedStringJpaConverter(null));
    }

    @Test
    void domainExceptionTypesExposeStableErrorCodes() {
        assertTrue(new VotingSessionValidationException("duplicate") instanceof DomainException);
        assertEquals("MIR-VOTING-SESSION-VALIDATION-001", new VotingSessionValidationException("duplicate").getErrorCode());
        assertEquals("MIR-VOTING-SESSION-STATE-001",
                new VotingSessionStateException(SessionStatus.CAST, SessionStatus.SPOILED).getErrorCode());
        assertEquals("message", new PiiEncryptionException("message").getMessage());
        assertEquals("message", new PiiEncryptionException("message", new RuntimeException("cause")).getMessage());
    }

    @Test
    void createVoterRecordStoresEncryptedExternalVoterIdAndEligibility() {
        PiiEncryptionService service = encryptionService();

        VoterRecord voter = VoterRecord.create(
                VOTER_ID,
                EXTERNAL_VOTER_ID,
                Set.of(ELECTION_ID),
                RegistrationStatus.ACTIVE,
                service);

        assertEquals(VOTER_ID, voter.getId());
        assertNotEquals(EXTERNAL_VOTER_ID, voter.getEncryptedExternalVoterId());
        assertEquals(EXTERNAL_VOTER_ID, voter.decryptExternalVoterId(service));
        assertEquals(Set.of(ELECTION_ID), voter.getEligibleElections());
        assertEquals(RegistrationStatus.ACTIVE, voter.getRegistrationStatus());
        assertTrue(voter.getVotingSessions().isEmpty());
    }

    @Test
    void voterRecordRejectsInvalidRequiredFields() {
        PiiEncryptionService service = encryptionService();

        assertThrows(NullPointerException.class, () -> VoterRecord.create(
                null, EXTERNAL_VOTER_ID, Set.of(ELECTION_ID), RegistrationStatus.ACTIVE, service));
        assertThrows(IllegalArgumentException.class, () -> VoterRecord.create(
                VOTER_ID, " ", Set.of(ELECTION_ID), RegistrationStatus.ACTIVE, service));
        assertThrows(IllegalArgumentException.class, () -> VoterRecord.create(
                VOTER_ID, EXTERNAL_VOTER_ID, Set.of(), RegistrationStatus.ACTIVE, service));
        assertThrows(NullPointerException.class, () -> VoterRecord.create(
                VOTER_ID, EXTERNAL_VOTER_ID, null, RegistrationStatus.ACTIVE, service));
        assertThrows(NullPointerException.class, () -> VoterRecord.create(
                VOTER_ID, EXTERNAL_VOTER_ID, Set.of(ELECTION_ID), null, service));
        assertThrows(NullPointerException.class, () -> VoterRecord.create(
                VOTER_ID, EXTERNAL_VOTER_ID, Set.of(ELECTION_ID), RegistrationStatus.ACTIVE, null));
    }

    @Test
    void voterRecordExposesImmutableEligibleElectionsAndSessions() {
        VoterRecord voter = voterRecord();
        voter.openVotingSession(SESSION_ID, election(), ballotStyle(), "DEVICE-001", OffsetDateTime.parse("2026-06-03T09:00:00Z"));

        assertThrows(UnsupportedOperationException.class, () -> voter.getEligibleElections().add(UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> voter.getVotingSessions().clear());
    }

    @Test
    void openVotingSessionInitializesFieldsWithElectionDayDefaultMethod() {
        VoterRecord voter = voterRecord();
        Election election = election();
        BallotStyle ballotStyle = ballotStyle();
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-06-03T09:00:00Z");

        VotingSession session = voter.openVotingSession(SESSION_ID, election, ballotStyle, "DEVICE-001", startedAt);

        assertEquals(SESSION_ID, session.getId());
        assertSame(voter, session.getVoterRecord());
        assertSame(election, session.getElection());
        assertSame(ballotStyle, session.getBallotStyle());
        assertEquals(startedAt, session.getStartedAt());
        assertEquals(null, session.getCompletedAt());
        assertEquals(SessionStatus.OPENED, session.getSessionStatus());
        assertEquals(VotingMethod.ELECTION_DAY, session.getVotingMethod());
        assertEquals("DEVICE-001", session.getDeviceId());
        assertEquals(1, voter.getVotingSessions().size());
    }

    @Test
    void openVotingSessionAcceptsExplicitVotingMethod() {
        VoterRecord voter = voterRecord();
        Election election = election();
        BallotStyle ballotStyle = ballotStyle();
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-29T09:00:00Z");

        VotingSession session = voter.openVotingSession(
                SESSION_ID, election, ballotStyle, "EARLY-SEOUL-001", startedAt, VotingMethod.EARLY_VOTING);

        assertEquals(VotingMethod.EARLY_VOTING, session.getVotingMethod());
    }

    @Test
    void openVotingSessionRejectsIneligibleElectionAndInvalidFields() {
        VoterRecord voter = voterRecord();
        Election election = Election.create(
                UUID.fromString("018f4b7e-e54a-7399-be53-d5e699614eba"),
                "Other Election",
                ElectionType.LOCAL,
                "Busan",
                LocalDate.of(2026, 6, 3),
                "KR",
                "ext-kr");
        election.pullDomainEvents();

        assertThrows(VotingSessionValidationException.class, () -> voter.openVotingSession(
                SESSION_ID, election, ballotStyle(), "DEVICE-001", OffsetDateTime.now()));
        assertThrows(NullPointerException.class, () -> voter.openVotingSession(
                null, election(), ballotStyle(), "DEVICE-001", OffsetDateTime.now()));
        assertThrows(NullPointerException.class, () -> voter.openVotingSession(
                SESSION_ID, null, ballotStyle(), "DEVICE-001", OffsetDateTime.now()));
        assertThrows(NullPointerException.class, () -> voter.openVotingSession(
                SESSION_ID, election(), null, "DEVICE-001", OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> voter.openVotingSession(
                SESSION_ID, election(), ballotStyle(), " ", OffsetDateTime.now()));
        assertThrows(NullPointerException.class, () -> voter.openVotingSession(
                SESSION_ID, election(), ballotStyle(), "DEVICE-001", OffsetDateTime.now(), null));
    }

    @Test
    void duplicatePreventionAllowsAtMostOneNonSpoiledSessionPerElection() {
        VoterRecord voter = voterRecord();
        Election election = election();
        BallotStyle ballotStyle = ballotStyle();

        voter.openVotingSession(SESSION_ID, election, ballotStyle, "DEVICE-001", OffsetDateTime.parse("2026-06-03T09:00:00Z"));

        VotingSessionValidationException exception = assertThrows(VotingSessionValidationException.class, () -> voter.openVotingSession(
                UUID.randomUUID(), election, ballotStyle, "DEVICE-002", OffsetDateTime.parse("2026-06-03T09:05:00Z")));
        assertEquals("MIR-VOTING-SESSION-VALIDATION-001", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("non-SPOILED"));
    }

    @Test
    void duplicatePreventionAllowsNewSessionAfterSpoiledSession() {
        VoterRecord voter = voterRecord();
        Election election = election();
        BallotStyle ballotStyle = ballotStyle();
        VotingSession spoiled = voter.openVotingSession(
                SESSION_ID, election, ballotStyle, "DEVICE-001", OffsetDateTime.parse("2026-06-03T09:00:00Z"));
        spoiled.spoil(OffsetDateTime.parse("2026-06-03T09:03:00Z"));

        VotingSession replacement = voter.openVotingSession(
                UUID.randomUUID(), election, ballotStyle, "DEVICE-002", OffsetDateTime.parse("2026-06-03T09:05:00Z"));

        assertEquals(SessionStatus.SPOILED, spoiled.getSessionStatus());
        assertEquals(SessionStatus.OPENED, replacement.getSessionStatus());
        assertEquals(2, voter.getVotingSessions().size());
    }

    @Test
    void votingSessionTerminalTransitionsSetCompletedAt() {
        OffsetDateTime completedAt = OffsetDateTime.parse("2026-06-03T09:10:00Z");
        VotingSession session = voterRecord().openVotingSession(
                SESSION_ID, election(), ballotStyle(), "DEVICE-001", OffsetDateTime.parse("2026-06-03T09:00:00Z"));

        session.cast(completedAt);

        assertEquals(SessionStatus.CAST, session.getSessionStatus());
        assertEquals(completedAt, session.getCompletedAt());
        assertThrows(VotingSessionStateException.class, () -> session.spoil(completedAt.plusMinutes(1)));
    }

    @Test
    void votingSessionCanExpireFromOpenedState() {
        OffsetDateTime completedAt = OffsetDateTime.parse("2026-06-03T09:30:00Z");
        VotingSession session = voterRecord().openVotingSession(
                SESSION_ID, election(), ballotStyle(), "DEVICE-001", OffsetDateTime.parse("2026-06-03T09:00:00Z"));

        session.expire(completedAt);

        assertEquals(SessionStatus.EXPIRED, session.getSessionStatus());
        assertEquals(completedAt, session.getCompletedAt());
    }

    @Test
    void terminalTransitionRequiresCompletedAt() {
        VotingSession session = voterRecord().openVotingSession(
                SESSION_ID, election(), ballotStyle(), "DEVICE-001", OffsetDateTime.parse("2026-06-03T09:00:00Z"));

        assertThrows(NullPointerException.class, () -> session.cast(null));
        assertEquals(SessionStatus.OPENED, session.getSessionStatus());
    }

    @Test
    void sessionStatusAllowsOnlyExpectedTransitions() {
        for (SessionStatus source : SessionStatus.values()) {
            for (SessionStatus target : SessionStatus.values()) {
                boolean expected = switch (source) {
                    case OPENED -> target == SessionStatus.CAST
                            || target == SessionStatus.SPOILED
                            || target == SessionStatus.EXPIRED;
                    case CAST, SPOILED, EXPIRED -> false;
                };

                assertEquals(expected, source.canTransitionTo(target), source + " -> " + target);
            }
        }
    }

    private static VoterRecord voterRecord() {
        return VoterRecord.create(
                VOTER_ID,
                EXTERNAL_VOTER_ID,
                Set.of(ELECTION_ID),
                RegistrationStatus.ACTIVE,
                encryptionService());
    }

    private static PiiEncryptionService encryptionService() {
        byte[] key = Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        return new PiiEncryptionService(key);
    }

    private static BallotStyle ballotStyle() {
        Ballot ballot = Ballot.create(BALLOT_ID, election());
        ballot.addContest(contest(), 1, "Mayor selection");
        return ballot.addStyle(STYLE_ID, "SEOUL-01-KO", "Seoul District 1", "ko", Set.of(AccessibilityFeature.LARGE_TEXT));
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
