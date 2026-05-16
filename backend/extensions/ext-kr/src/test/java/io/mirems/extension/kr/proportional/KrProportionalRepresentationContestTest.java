package io.mirems.extension.kr.proportional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mirems.extension.kr.KrElectionType;
import org.junit.jupiter.api.Test;

class KrProportionalRepresentationContestTest {

    @Test
    void definesKrParliamentaryProportionalRepresentationContestType() {
        assertThat(KrProportionalRepresentationContest.CONTEST_TYPE_CODE)
                .isEqualTo("PROPORTIONAL_REPRESENTATION");
        assertThat(KrProportionalRepresentationContest.KOREAN_LABEL)
                .isEqualTo("비례대표");
        assertThat(KrProportionalRepresentationContest.supportedElectionType())
                .isEqualTo(KrElectionType.NATIONAL_ASSEMBLY_ELECTION);
        assertThat(KrProportionalRepresentationContest.voteLimit()).isEqualTo(1);
    }

    @Test
    void createsContestDefinitionForPositiveSeatCount() {
        KrProportionalRepresentationContest contest = KrProportionalRepresentationContest.create(47);

        assertThat(contest.code()).isEqualTo("PROPORTIONAL_REPRESENTATION");
        assertThat(contest.title()).isEqualTo("비례대표 국회의원선거");
        assertThat(contest.seats()).isEqualTo(47);
        assertThat(contest.voteLimit()).isEqualTo(1);
    }

    @Test
    void rejectsNonPositiveSeatCount() {
        assertThatThrownBy(() -> KrProportionalRepresentationContest.create(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("proportional representation seats must be greater than zero");
    }
}
