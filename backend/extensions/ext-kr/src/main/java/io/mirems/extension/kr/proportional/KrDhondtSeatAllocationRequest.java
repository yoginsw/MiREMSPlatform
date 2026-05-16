package io.mirems.extension.kr.proportional;

import java.util.List;

public record KrDhondtSeatAllocationRequest(int totalSeats, List<KrPartyVote> partyVotes) {
    public KrDhondtSeatAllocationRequest {
        partyVotes = List.copyOf(partyVotes);
    }
}
