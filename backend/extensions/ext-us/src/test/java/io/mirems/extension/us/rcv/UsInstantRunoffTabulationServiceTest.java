package io.mirems.extension.us.rcv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsInstantRunoffTabulationServiceTest {
    private static final UUID CONTEST_ID = UUID.fromString("018f4c00-0000-7000-8000-000000000001");
    private static final UUID ALICE = UUID.fromString("018f4c00-0000-7000-8000-000000000101");
    private static final UUID BOB = UUID.fromString("018f4c00-0000-7000-8000-000000000102");
    private static final UUID CAROL = UUID.fromString("018f4c00-0000-7000-8000-000000000103");
    private static final UUID DAVE = UUID.fromString("018f4c00-0000-7000-8000-000000000104");
    private static final Set<UUID> CANDIDATES = Set.of(ALICE, BOB, CAROL);

    private final UsInstantRunoffTabulationService service = new UsInstantRunoffTabulationService();

    @Test
    void instantRunoffElectsKnownWinnerAfterLowestCandidateTransfers() {
        UsInstantRunoffResult result = service.tabulate(new UsInstantRunoffRequest(CONTEST_ID, CANDIDATES, List.of(
                ballot(ALICE, BOB, CAROL),
                ballot(ALICE, BOB, CAROL),
                ballot(ALICE, BOB, CAROL),
                ballot(ALICE, CAROL, BOB),
                ballot(BOB, ALICE, CAROL),
                ballot(BOB, ALICE, CAROL),
                ballot(BOB, CAROL, ALICE),
                ballot(CAROL, BOB, ALICE),
                ballot(CAROL, BOB, ALICE))));

        assertThat(result.winnerCandidateId()).contains(BOB);
        assertThat(result.exhaustedBallots()).isZero();
        assertThat(result.rounds()).hasSize(2);
        assertThat(result.rounds().get(0).activeTallies()).isEqualTo(Map.of(ALICE, 4, BOB, 3, CAROL, 2));
        assertThat(result.rounds().get(0).eliminatedCandidateId()).contains(CAROL);
        assertThat(result.rounds().get(1).activeTallies()).isEqualTo(Map.of(ALICE, 4, BOB, 5));
        assertThat(result.rounds().get(1).majorityThreshold()).isEqualTo(5);
        assertThat(result.auditSummary()).contains("winner=018f4c00-0000-7000-8000-000000000102");
    }

    @Test
    void instantRunoffTracksExhaustedBallotsAndDeterministicTieBreakElimination() {
        UsInstantRunoffResult result = service.tabulate(new UsInstantRunoffRequest(CONTEST_ID, Set.of(ALICE, BOB, CAROL, DAVE), List.of(
                ballot(ALICE),
                ballot(BOB, ALICE),
                ballot(CAROL, BOB),
                ballot(DAVE))));

        assertThat(result.rounds().get(0).activeTallies()).isEqualTo(Map.of(ALICE, 1, BOB, 1, CAROL, 1, DAVE, 1));
        assertThat(result.rounds().get(0).eliminatedCandidateId()).contains(DAVE);
        assertThat(result.exhaustedBallots()).isEqualTo(1);
        assertThat(result.winnerCandidateId()).contains(BOB);
    }

    @Test
    void instantRunoffRejectsEmptyCandidatesEmptyBallotsAndMixedContestBallots() {
        assertThatThrownBy(() -> service.tabulate(new UsInstantRunoffRequest(CONTEST_ID, Set.of(), List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("candidateIds must not be empty");
        assertThatThrownBy(() -> service.tabulate(new UsInstantRunoffRequest(CONTEST_ID, CANDIDATES, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ballots must not be empty");
        UsRankedChoiceBallot otherContestBallot = UsRankedChoiceBallot.cast(
                UUID.fromString("018f4c00-0000-7000-8000-000000000099"), List.of(ALICE), CANDIDATES);
        assertThatThrownBy(() -> service.tabulate(new UsInstantRunoffRequest(CONTEST_ID, CANDIDATES, List.of(otherContestBallot))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ballot contestId must match request contestId");
    }

    private static UsRankedChoiceBallot ballot(UUID... rankedCandidateIds) {
        return UsRankedChoiceBallot.cast(CONTEST_ID, List.of(rankedCandidateIds), Set.of(ALICE, BOB, CAROL, DAVE));
    }
}
