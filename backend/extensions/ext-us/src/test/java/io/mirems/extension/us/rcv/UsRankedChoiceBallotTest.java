package io.mirems.extension.us.rcv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsRankedChoiceBallotTest {
    private static final UUID CONTEST_ID = UUID.fromString("018f4c00-0000-7000-8000-000000000001");
    private static final UUID ALICE = UUID.fromString("018f4c00-0000-7000-8000-000000000101");
    private static final UUID BOB = UUID.fromString("018f4c00-0000-7000-8000-000000000102");
    private static final UUID CAROL = UUID.fromString("018f4c00-0000-7000-8000-000000000103");
    private static final Set<UUID> CANDIDATES = Set.of(ALICE, BOB, CAROL);

    @Test
    void rankedChoiceBallotPreservesVoterRankOrderForOneToNCandidates() {
        UsRankedChoiceBallot ballot = UsRankedChoiceBallot.cast(CONTEST_ID, List.of(ALICE, BOB, CAROL), CANDIDATES);

        assertThat(ballot.contestId()).isEqualTo(CONTEST_ID);
        assertThat(ballot.rankedCandidateIds()).containsExactly(ALICE, BOB, CAROL);
        assertThat(ballot.firstActiveChoice(Set.of(ALICE, BOB, CAROL))).contains(ALICE);
        assertThat(ballot.firstActiveChoice(Set.of(BOB, CAROL))).contains(BOB);
    }

    @Test
    void rankedChoiceBallotRejectsDuplicateRanksUnknownCandidatesAndTooManyRanks() {
        assertThatThrownBy(() -> new UsRankedChoiceBallot(CONTEST_ID, List.of(ALICE, ALICE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rankedCandidateIds must not contain duplicates");
        assertThatThrownBy(() -> UsRankedChoiceBallot.cast(CONTEST_ID, List.of(ALICE, ALICE), CANDIDATES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rankedCandidateIds must not contain duplicates");
        assertThatThrownBy(() -> UsRankedChoiceBallot.cast(CONTEST_ID, List.of(ALICE, UUID.fromString("018f4c00-0000-7000-8000-000000000999")), CANDIDATES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rankedCandidateIds contains a candidate outside the contest");
        assertThatThrownBy(() -> UsRankedChoiceBallot.cast(CONTEST_ID, List.of(ALICE, BOB, CAROL), Set.of(ALICE, BOB)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rankedCandidateIds cannot exceed candidate count");
    }
}
