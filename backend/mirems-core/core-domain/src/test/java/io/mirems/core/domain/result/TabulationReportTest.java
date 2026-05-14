package io.mirems.core.domain.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.junit.jupiter.api.Test;

class TabulationReportTest {
    private static final UUID REPORT_ID = UUID.fromString("018f4b80-0000-7000-8000-000000000001");
    private static final UUID ELECTION_ID = UUID.fromString("018f4b80-0000-7000-8000-000000000002");
    private static final UUID CONTEST_ID = UUID.fromString("018f4b80-0000-7000-8000-000000000003");
    private static final UUID CANDIDATE_ONE = UUID.fromString("018f4b80-0000-7000-8000-000000000004");
    private static final UUID CANDIDATE_TWO = UUID.fromString("018f4b80-0000-7000-8000-000000000005");
    private static final OffsetDateTime GENERATED_AT = OffsetDateTime.parse("2026-06-03T20:00:00Z");

    @Test
    void tabulationReportIsImmutableJpaEntityWithNoSetters() {
        assertNotNull(TabulationReport.class.getAnnotation(Immutable.class));

        List<String> setterNames = Arrays.stream(TabulationReport.class.getDeclaredMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .toList();

        assertTrue(setterNames.isEmpty());
    }

    @Test
    void persistedColumnsAreNotUpdatable() throws Exception {
        assertColumnNotUpdatable("id");
        assertColumnNotUpdatable("electionId");
        assertColumnNotUpdatable("contestTallies");
        assertColumnNotUpdatable("generatedAt");
        assertColumnNotUpdatable("lockedAt");
        assertColumnNotUpdatable("hash");
        assertColumnNotUpdatable("published");
    }

    @Test
    void draftReportStoresImmutableContestTallies() {
        TabulationReport report = draftReport();

        assertEquals(REPORT_ID, report.getId());
        assertEquals(ELECTION_ID, report.getElectionId());
        assertEquals(GENERATED_AT, report.getGeneratedAt());
        assertEquals(null, report.getLockedAt());
        assertEquals(null, report.getHash());
        assertFalse(report.isLocked());
        assertFalse(report.isPublished());
        assertEquals(2, report.totalBallotsCounted());
        assertEquals(2, report.getContestTallies().get(CONTEST_ID).candidateTallies().get(CANDIDATE_ONE));
        assertEquals(1, report.getContestTallies().get(CONTEST_ID).candidateTallies().get(CANDIDATE_TWO));
        assertThrows(UnsupportedOperationException.class, () -> report.getContestTallies().clear());
    }

    @Test
    void lockComputesDeterministicSha256HashAndEmitsCompletionEvent() {
        TabulationReport left = draftReport();
        TabulationReport right = draftReport();

        TabulationCompletedEvent event = left.lock(GENERATED_AT.plusHours(1));
        right.lock(GENERATED_AT.plusHours(1));

        assertTrue(left.isLocked());
        assertEquals(GENERATED_AT.plusHours(1), left.getLockedAt());
        assertEquals(64, left.getHash().length());
        assertEquals(left.getHash(), right.getHash());
        assertTrue(left.verifyHash());
        assertEquals(REPORT_ID, event.reportId());
        assertEquals(ELECTION_ID, event.electionId());
        assertEquals(left.getHash(), event.reportHash());
        assertEquals(GENERATED_AT.plusHours(1), event.completedAt());
    }

    @Test
    void hashChangesWhenCommittedTalliesChange() {
        TabulationReport oneVote = TabulationReport.draft(
                REPORT_ID,
                ELECTION_ID,
                Map.of(CONTEST_ID, new ContestTally(CONTEST_ID, Map.of(CANDIDATE_ONE, 1), 1)),
                GENERATED_AT);
        TabulationReport twoVotes = draftReport();

        oneVote.lock(GENERATED_AT.plusHours(1));
        twoVotes.lock(GENERATED_AT.plusHours(1));

        assertFalse(oneVote.getHash().equals(twoVotes.getHash()));
    }

    @Test
    void prePersistLifecycleHookLocksDraftReport() throws Exception {
        Method method = TabulationReport.class.getDeclaredMethod("computeHashBeforePersist");
        assertNotNull(method.getAnnotation(PrePersist.class));
        TabulationReport report = draftReport();

        method.invoke(report);

        assertTrue(report.isLocked());
        assertNotNull(report.getHash());
        assertTrue(report.verifyHash());
    }

    @Test
    void repositoryExposesOnlySaveAndFindMethods() {
        List<String> methodNames = Arrays.stream(TabulationReportRepository.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        assertTrue(methodNames.contains("save"));
        assertTrue(methodNames.contains("findById"));
        assertTrue(methodNames.contains("findByElectionId"));
        assertTrue(methodNames.stream().allMatch(name -> name.equals("save") || name.startsWith("findBy")));
        assertFalse(methodNames.stream().anyMatch(name -> name.startsWith("update") || name.startsWith("delete")));
    }

    @Test
    void reportRejectsInvalidRequiredFields() {
        assertThrows(NullPointerException.class, () -> TabulationReport.draft(null, ELECTION_ID, tallies(), GENERATED_AT));
        assertThrows(NullPointerException.class, () -> TabulationReport.draft(REPORT_ID, null, tallies(), GENERATED_AT));
        assertThrows(NullPointerException.class, () -> TabulationReport.draft(REPORT_ID, ELECTION_ID, null, GENERATED_AT));
        assertThrows(IllegalArgumentException.class, () -> TabulationReport.draft(REPORT_ID, ELECTION_ID, Map.of(), GENERATED_AT));
        assertThrows(NullPointerException.class, () -> TabulationReport.draft(REPORT_ID, ELECTION_ID, tallies(), null));
    }

    private static void assertColumnNotUpdatable(String fieldName) throws Exception {
        Column column = TabulationReport.class.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertNotNull(column, fieldName + " must have @Column");
        assertFalse(column.updatable(), fieldName + " must not be updatable");
    }

    private static TabulationReport draftReport() {
        return TabulationReport.draft(REPORT_ID, ELECTION_ID, tallies(), GENERATED_AT);
    }

    private static Map<UUID, ContestTally> tallies() {
        return Map.of(CONTEST_ID, new ContestTally(CONTEST_ID, Map.of(CANDIDATE_ONE, 2, CANDIDATE_TWO, 1), 2));
    }
}
