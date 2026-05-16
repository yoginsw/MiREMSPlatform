package io.mirems.extension.kr.proportional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class KrPartyListBallotRendererTest {

    private final KrPartyListBallotRenderer renderer = new KrPartyListBallotRenderer();

    @Test
    void rendersPartyListBallotLayoutSortedByListOrder() {
        KrPartyListBallotLayout layout = renderer.render(KrProportionalRepresentationContest.create(47), List.of(
                new KrPartyListOption("party-b", "나무당", "/logos/tree.svg", 2),
                new KrPartyListOption("party-a", "가람당", "/logos/river.svg", 1)));

        assertThat(layout.contestType()).isEqualTo("PROPORTIONAL_REPRESENTATION");
        assertThat(layout.title()).isEqualTo("비례대표 국회의원선거");
        assertThat(layout.instructions()).isEqualTo("정당명부 중 하나의 정당만 선택해 주세요.");
        assertThat(layout.type()).isEqualTo("single");
        assertThat(layout.maxSelections()).isEqualTo(1);
        assertThat(layout.options()).extracting(KrPartyListBallotLayout.Option::id)
                .containsExactly("party-a", "party-b");
        assertThat(layout.options()).extracting(KrPartyListBallotLayout.Option::label)
                .containsExactly("가람당", "나무당");
        assertThat(layout.options()).extracting(KrPartyListBallotLayout.Option::partyLogoUri)
                .containsExactly("/logos/river.svg", "/logos/tree.svg");
    }

    @Test
    void rejectsEmptyPartyList() {
        assertThatThrownBy(() -> renderer.render(KrProportionalRepresentationContest.create(47), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("party list must not be empty");
    }

    @Test
    void rejectsDuplicatePartyIds() {
        KrPartyListOption first = new KrPartyListOption("party-a", "가람당", "/logos/river.svg", 1);
        KrPartyListOption duplicate = new KrPartyListOption("party-a", "가람당2", "/logos/river2.svg", 2);

        assertThatThrownBy(() -> renderer.render(KrProportionalRepresentationContest.create(47), List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("party list contains duplicate party id: party-a");
    }
}
