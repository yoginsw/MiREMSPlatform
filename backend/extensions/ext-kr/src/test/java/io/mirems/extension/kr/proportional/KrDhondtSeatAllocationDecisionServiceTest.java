package io.mirems.extension.kr.proportional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class KrDhondtSeatAllocationDecisionServiceTest {

    private final KrDhondtSeatAllocationDecisionService service = new KrDhondtSeatAllocationDecisionService();

    @Test
    void allocatesSeatsWithDhondtMethodForKnownFiveSeatCase() {
        KrDhondtSeatAllocationResult result = service.allocate(new KrDhondtSeatAllocationRequest(5, List.of(
                new KrPartyVote("party-a", "가람당", 100_000),
                new KrPartyVote("party-b", "나무당", 80_000),
                new KrPartyVote("party-c", "누리당", 30_000))));

        assertThat(result.allocations()).extracting(KrPartySeatAllocation::partyId)
                .containsExactly("party-a", "party-b", "party-c");
        assertThat(result.allocations()).extracting(KrPartySeatAllocation::allocatedSeats)
                .containsExactly(3, 2, 0);
        assertThat(result.totalSeatsAllocated()).isEqualTo(5);
        assertThat(result.reason()).isEqualTo("allocated by D'Hondt highest quotients");
    }

    @Test
    void allocatesSeatsWithDhondtMethodForKnownTenSeatCase() {
        KrDhondtSeatAllocationResult result = service.allocate(new KrDhondtSeatAllocationRequest(10, List.of(
                new KrPartyVote("party-a", "가람당", 340_000),
                new KrPartyVote("party-b", "나무당", 280_000),
                new KrPartyVote("party-c", "누리당", 160_000),
                new KrPartyVote("party-d", "다솜당", 80_000))));

        assertThat(result.allocations()).extracting(KrPartySeatAllocation::allocatedSeats)
                .containsExactly(4, 3, 2, 1);
        assertThat(result.totalSeatsAllocated()).isEqualTo(10);
    }

    @Test
    void resolvesEqualQuotientTiesByHigherTotalVotesThenPartyId() {
        KrDhondtSeatAllocationResult result = service.allocate(new KrDhondtSeatAllocationRequest(3, List.of(
                new KrPartyVote("party-b", "나무당", 100),
                new KrPartyVote("party-a", "가람당", 100),
                new KrPartyVote("party-c", "누리당", 50))));

        assertThat(result.allocations()).extracting(KrPartySeatAllocation::partyId)
                .containsExactly("party-a", "party-b", "party-c");
        assertThat(result.allocations()).extracting(KrPartySeatAllocation::allocatedSeats)
                .containsExactly(2, 1, 0);
    }

    @Test
    void rejectsInvalidRequestsBeforeAllocation() {
        assertThatThrownBy(() -> service.allocate(new KrDhondtSeatAllocationRequest(0, List.of(
                new KrPartyVote("party-a", "가람당", 100)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("totalSeats must be greater than zero");

        assertThatThrownBy(() -> service.allocate(new KrDhondtSeatAllocationRequest(1, List.of(
                new KrPartyVote("party-a", "가람당", -1)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("party votes must not be negative");
    }
}
