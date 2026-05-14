package io.mirems.core.domain.result;

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
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;

class VoteCorrectionTest {
    private static final UUID RESULT_ID = UUID.fromString("018f4bc0-1111-7222-8333-944455556666");
    private static final UUID CORRECTION_ID = UUID.fromString("018f4bc0-2222-7333-8444-a55566667777");
    private static final UUID CANDIDATE_ONE = UUID.fromString("018f4bc0-3333-7444-8555-b66677778888");
    private static final UUID CANDIDATE_TWO = UUID.fromString("018f4bc0-4444-7555-8666-c77788889999");
    private static final OffsetDateTime CAST_AT = OffsetDateTime.parse("2026-06-03T09:30:00Z");

    @Test
    void voteCorrectionIsImmutableJpaEntityWithNoSetters() throws Exception {
        assertNotNull(VoteCorrection.class.getAnnotation(Immutable.class));
        List<String> setterNames = Arrays.stream(VoteCorrection.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();
        assertTrue(setterNames.isEmpty());

        assertColumnNotUpdatable("id");
        assertColumnNotUpdatable("correctedCandidateIds");
        assertColumnNotUpdatable("reason");
        assertColumnNotUpdatable("requestedBy");
        assertColumnNotUpdatable("requestedAt");
        assertColumnNotUpdatable("correctionStatus");
        assertColumnNotUpdatable("firstApprovedBy");
        assertColumnNotUpdatable("firstApprovedAt");
        assertColumnNotUpdatable("secondApprovedBy");
        assertColumnNotUpdatable("secondApprovedAt");
        assertJoinColumnNotUpdatable("originalVotingResult");
    }

    @Test
    void dualApprovalRequiresTwoDistinctElectionAdminsAndRecordsAuditTrail() {
        VotingResult original = originalResult();
        original.computeHashBeforePersist();
        String originalHash = original.getHash();
        VoteCorrection correction = VoteCorrection.request(
                CORRECTION_ID,
                original,
                List.of(CANDIDATE_TWO),
                "Administrative data entry correction",
                "operator-001",
                CAST_AT.plusHours(1));

        correction.recordFirstApproval("admin-a", CAST_AT.plusHours(2));
        VoteCorrectedEvent event = correction.recordSecondApproval("admin-b", CAST_AT.plusHours(3));

        assertEquals(CorrectionStatus.APPROVED, correction.getCorrectionStatus());
        assertEquals("admin-a", correction.getFirstApprovedBy());
        assertEquals(CAST_AT.plusHours(2), correction.getFirstApprovedAt());
        assertEquals("admin-b", correction.getSecondApprovedBy());
        assertEquals(CAST_AT.plusHours(3), correction.getSecondApprovedAt());
        assertEquals(CORRECTION_ID, event.correctionId());
        assertEquals(RESULT_ID, event.originalVotingResultId());
        assertEquals(List.of(CANDIDATE_ONE), event.originalCandidateIds());
        assertEquals(List.of(CANDIDATE_TWO), event.correctedCandidateIds());
        assertEquals("operator-001", event.requestedBy());
        assertEquals("admin-a", event.firstApprovedBy());
        assertEquals("admin-b", event.secondApprovedBy());
        assertEquals(originalHash, original.getHash());
        assertTrue(original.verifyHash());
    }

    @Test
    void secondApprovalRejectsSameApproverOrMissingFirstApproval() {
        VoteCorrection correction = requestedCorrection();

        assertThrows(IllegalStateException.class, () -> correction.recordSecondApproval("admin-a", CAST_AT.plusHours(3)));

        correction.recordFirstApproval("admin-a", CAST_AT.plusHours(2));

        assertThrows(IllegalArgumentException.class, () -> correction.recordSecondApproval("admin-a", CAST_AT.plusHours(3)));
    }

    @Test
    void approvedCorrectionCannotBeApprovedAgain() {
        VoteCorrection correction = requestedCorrection();
        correction.recordFirstApproval("admin-a", CAST_AT.plusHours(2));
        correction.recordSecondApproval("admin-b", CAST_AT.plusHours(3));

        assertThrows(IllegalStateException.class, () -> correction.recordFirstApproval("admin-c", CAST_AT.plusHours(4)));
        assertThrows(IllegalStateException.class, () -> correction.recordSecondApproval("admin-c", CAST_AT.plusHours(4)));
    }

    @Test
    void repositoryPortIsAppendOnly() {
        List<String> methodNames = Arrays.stream(VoteCorrectionRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertTrue(methodNames.contains("save"));
        assertTrue(methodNames.contains("findById"));
        assertTrue(methodNames.contains("findByOriginalVotingResultId"));
        assertTrue(methodNames.stream().allMatch(name -> name.equals("save") || name.startsWith("findBy")));
        assertFalse(methodNames.stream().anyMatch(name -> name.startsWith("update") || name.startsWith("delete")));
    }

    private static VoteCorrection requestedCorrection() {
        return VoteCorrection.request(
                CORRECTION_ID,
                originalResult(),
                List.of(CANDIDATE_TWO),
                "Administrative data entry correction",
                "operator-001",
                CAST_AT.plusHours(1));
    }

    private static VotingResult originalResult() {
        return VotingResult.create(RESULT_ID, votingSession(), contest(), List.of(CANDIDATE_ONE), CAST_AT);
    }

    private static void assertColumnNotUpdatable(String fieldName) throws Exception {
        Column column = VoteCorrection.class.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertNotNull(column, fieldName + " must have @Column");
        assertFalse(column.updatable(), fieldName + " must not be updatable");
    }

    private static void assertJoinColumnNotUpdatable(String fieldName) throws Exception {
        JoinColumn joinColumn = VoteCorrection.class.getDeclaredField(fieldName).getAnnotation(JoinColumn.class);
        assertNotNull(joinColumn, fieldName + " must have @JoinColumn");
        assertFalse(joinColumn.updatable(), fieldName + " must not be updatable");
    }

    private static VotingSession votingSession() {
        return voterRecord().openVotingSession(
                UUID.fromString("018f4bc0-5555-7666-8777-d88899990000"),
                election(),
                ballotStyle(),
                "DEVICE-001",
                CAST_AT.minusMinutes(10));
    }

    private static VoterRecord voterRecord() {
        return VoterRecord.create(
                UUID.fromString("018f4bc0-6666-7777-8888-e99900001111"),
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
        Ballot ballot = Ballot.create(UUID.fromString("018f4bc0-7777-7888-8999-f00011112222"), election());
        ballot.addContest(contest(), 1, "Mayor selection");
        return ballot.addStyle(
                UUID.fromString("018f4bc0-8888-7999-8aaa-011122223333"),
                "SEOUL-01-KO",
                "Seoul District 1",
                "ko",
                Set.of(AccessibilityFeature.LARGE_TEXT));
    }

    private static Contest contest() {
        return Contest.create(
                UUID.fromString("018f4bc0-9999-7aaa-8bbb-122233334444"),
                election(),
                ContestType.CANDIDATE_CHOICE,
                "Mayor",
                1,
                1);
    }

    private static Election election() {
        Election election = Election.create(
                UUID.fromString("018f4bc0-aaaa-7bbb-8ccc-233344445555"),
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
