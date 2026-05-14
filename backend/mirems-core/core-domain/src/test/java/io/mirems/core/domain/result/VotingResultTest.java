package io.mirems.core.domain.result;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.VotingSession;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;

class VotingResultTest {
    private static final UUID RESULT_ID = UUID.fromString("018f4b7f-1111-7222-8333-944455556666");
    private static final UUID CORRECTION_ID = UUID.fromString("018f4b7f-2222-7333-8444-a55566667777");
    private static final UUID CANDIDATE_ONE = UUID.fromString("018f4b7f-3333-7444-8555-b66677778888");
    private static final UUID CANDIDATE_TWO = UUID.fromString("018f4b7f-4444-7555-8666-c77788889999");
    private static final OffsetDateTime CAST_AT = OffsetDateTime.parse("2026-06-03T09:30:00Z");

    @Test
    void votingResultIsImmutableJpaEntityWithNoSetters() {
        assertNotNull(VotingResult.class.getAnnotation(Immutable.class));

        List<String> setterNames = Arrays.stream(VotingResult.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertTrue(setterNames.isEmpty());
    }

    @Test
    void persistedColumnsAndJoinColumnsAreNotUpdatable() throws Exception {
        assertColumnNotUpdatable("id");
        assertColumnNotUpdatable("selectedCandidateIds");
        assertColumnNotUpdatable("castAt");
        assertColumnNotUpdatable("hash");
        assertJoinColumnNotUpdatable("session");
        assertJoinColumnNotUpdatable("contest");
    }

    @Test
    void createVotingResultInitializesFieldsAndImmutableCandidateList() {
        VotingSession session = votingSession();
        Contest contest = contest();

        VotingResult result = VotingResult.create(RESULT_ID, session, contest, List.of(CANDIDATE_ONE, CANDIDATE_TWO), CAST_AT);

        assertEquals(RESULT_ID, result.getId());
        assertEquals(session, result.getSession());
        assertEquals(contest, result.getContest());
        assertEquals(List.of(CANDIDATE_ONE, CANDIDATE_TWO), result.getSelectedCandidateIds());
        assertEquals(CAST_AT, result.getCastAt());
        assertEquals(null, result.getHash());
        assertThrows(UnsupportedOperationException.class, () -> result.getSelectedCandidateIds().add(UUID.randomUUID()));
    }

    @Test
    void votingResultRejectsInvalidRequiredFields() {
        VotingSession session = votingSession();
        Contest contest = contest();

        assertThrows(NullPointerException.class,
                () -> VotingResult.create(null, session, contest, List.of(CANDIDATE_ONE), CAST_AT));
        assertThrows(NullPointerException.class,
                () -> VotingResult.create(RESULT_ID, null, contest, List.of(CANDIDATE_ONE), CAST_AT));
        assertThrows(NullPointerException.class,
                () -> VotingResult.create(RESULT_ID, session, null, List.of(CANDIDATE_ONE), CAST_AT));
        assertThrows(NullPointerException.class,
                () -> VotingResult.create(RESULT_ID, session, contest, null, CAST_AT));
        assertThrows(IllegalArgumentException.class,
                () -> VotingResult.create(RESULT_ID, session, contest, List.of(), CAST_AT));
        assertThrows(NullPointerException.class,
                () -> VotingResult.create(RESULT_ID, session, contest, List.of(CANDIDATE_ONE), null));
    }

    @Test
    void prePersistComputesDeterministicSha256Hash() {
        VotingResult left = VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE, CANDIDATE_TWO), CAST_AT);
        VotingResult right = VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE, CANDIDATE_TWO), CAST_AT);

        left.computeHashBeforePersist();
        right.computeHashBeforePersist();

        assertEquals(left.getHash(), right.getHash());
        assertEquals(64, left.getHash().length());
        assertTrue(left.verifyHash());
    }

    @Test
    void hashChangesWhenCommittedFieldsChange() {
        VotingResult oneSelection = VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE), CAST_AT);
        VotingResult twoSelections = VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE, CANDIDATE_TWO), CAST_AT);

        oneSelection.computeHashBeforePersist();
        twoSelections.computeHashBeforePersist();

        assertFalse(oneSelection.getHash().equals(twoSelections.getHash()));
    }

    @Test
    void prePersistLifecycleHookExistsAndCanBeInvoked() throws Exception {
        Method method = VotingResult.class.getDeclaredMethod("computeHashBeforePersist");
        assertNotNull(method.getAnnotation(PrePersist.class));
        VotingResult result = VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE), CAST_AT);

        method.invoke(result);

        assertNotNull(result.getHash());
    }

    @Test
    void repositoryExposesOnlySaveAndFindMethods() {
        List<String> methodNames = Arrays.stream(VotingResultRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertTrue(methodNames.contains("save"));
        assertTrue(methodNames.contains("findById"));
        assertTrue(methodNames.contains("findBySessionId"));
        assertTrue(methodNames.contains("findByContestId"));
        assertTrue(methodNames.stream().allMatch(name -> name.equals("save") || name.startsWith("findBy")));
        assertFalse(methodNames.stream().anyMatch(name -> name.startsWith("update") || name.startsWith("delete")));
    }

    @Test
    void voteCorrectionReferencesOriginalResultWithoutMutatingIt() {
        VotingResult original = VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE), CAST_AT);
        original.computeHashBeforePersist();
        String originalHash = original.getHash();

        VoteCorrection correction = VoteCorrection.request(
                CORRECTION_ID,
                original,
                List.of(CANDIDATE_TWO),
                "Operator selected wrong candidate during adjudication",
                "admin-001",
                CAST_AT.plusHours(2));

        assertEquals(CORRECTION_ID, correction.getId());
        assertEquals(original, correction.getOriginalVotingResult());
        assertEquals(List.of(CANDIDATE_TWO), correction.getCorrectedCandidateIds());
        assertEquals("Operator selected wrong candidate during adjudication", correction.getReason());
        assertEquals("admin-001", correction.getRequestedBy());
        assertEquals(CAST_AT.plusHours(2), correction.getRequestedAt());
        assertEquals(CorrectionStatus.PENDING_APPROVAL, correction.getCorrectionStatus());
        assertEquals(originalHash, original.getHash());
        assertThrows(UnsupportedOperationException.class, () -> correction.getCorrectedCandidateIds().clear());
    }

    @Test
    void jpaConstructorsExistForImmutableEntities() throws Exception {
        Constructor<VotingResult> votingResultConstructor = VotingResult.class.getDeclaredConstructor();
        votingResultConstructor.setAccessible(true);
        Constructor<VoteCorrection> correctionConstructor = VoteCorrection.class.getDeclaredConstructor();
        correctionConstructor.setAccessible(true);

        assertNotNull(assertDoesNotThrow(() -> votingResultConstructor.newInstance()));
        assertNotNull(assertDoesNotThrow(() -> correctionConstructor.newInstance()));
    }

    @Test
    void voteCorrectionRejectsInvalidRequiredFields() {
        VotingResult original = VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE), CAST_AT);

        assertThrows(NullPointerException.class,
                () -> VoteCorrection.request(null, original, List.of(CANDIDATE_TWO), "reason", "admin", CAST_AT));
        assertThrows(NullPointerException.class,
                () -> VoteCorrection.request(CORRECTION_ID, null, List.of(CANDIDATE_TWO), "reason", "admin", CAST_AT));
        assertThrows(IllegalArgumentException.class,
                () -> VoteCorrection.request(CORRECTION_ID, original, List.of(), "reason", "admin", CAST_AT));
        assertThrows(IllegalArgumentException.class,
                () -> VoteCorrection.request(CORRECTION_ID, original, List.of(CANDIDATE_TWO), " ", "admin", CAST_AT));
        assertThrows(IllegalArgumentException.class,
                () -> VoteCorrection.request(CORRECTION_ID, original, List.of(CANDIDATE_TWO), "reason", " ", CAST_AT));
        assertThrows(NullPointerException.class,
                () -> VoteCorrection.request(CORRECTION_ID, original, List.of(CANDIDATE_TWO), "reason", "admin", null));
    }

    private static void assertColumnNotUpdatable(String fieldName) throws Exception {
        Column column = VotingResult.class.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertNotNull(column, fieldName + " must have @Column");
        assertFalse(column.updatable(), fieldName + " must not be updatable");
    }

    private static void assertJoinColumnNotUpdatable(String fieldName) throws Exception {
        JoinColumn joinColumn = VotingResult.class.getDeclaredField(fieldName).getAnnotation(JoinColumn.class);
        assertNotNull(joinColumn, fieldName + " must have @JoinColumn");
        assertFalse(joinColumn.updatable(), fieldName + " must not be updatable");
    }

    private static VotingSession votingSession() {
        return voterRecord().openVotingSession(
                UUID.fromString("018f4b7f-5555-7666-8777-d88899990000"),
                election(),
                ballotStyle(),
                "DEVICE-001",
                CAST_AT.minusMinutes(10));
    }

    private static VoterRecord voterRecord() {
        return VoterRecord.create(
                UUID.fromString("018f4b7f-6666-7777-8888-e99900001111"),
                "KR-SEOUL-000001",
                Set.of(election().getId()),
                RegistrationStatus.ACTIVE,
                encryptionService());
    }

    private static PiiEncryptionService encryptionService() {
        byte[] key = Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        return new PiiEncryptionService(key);
    }

    private static BallotStyle ballotStyle() {
        Ballot ballot = Ballot.create(UUID.fromString("018f4b7f-7777-7888-8999-f00011112222"), election());
        ballot.addContest(contest(), 1, "Mayor selection");
        return ballot.addStyle(
                UUID.fromString("018f4b7f-8888-7999-8aaa-011122223333"),
                "SEOUL-01-KO",
                "Seoul District 1",
                "ko",
                Set.of(AccessibilityFeature.LARGE_TEXT));
    }

    private static Contest contest() {
        return Contest.create(
                UUID.fromString("018f4b7f-9999-7aaa-8bbb-122233334444"),
                election(),
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1);
    }

    private static Election election() {
        Election election = Election.create(
                UUID.fromString("018f4b7f-aaaa-7bbb-8ccc-233344445555"),
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
