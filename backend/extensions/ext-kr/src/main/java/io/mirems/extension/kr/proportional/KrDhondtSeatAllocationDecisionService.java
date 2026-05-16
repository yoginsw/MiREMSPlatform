package io.mirems.extension.kr.proportional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class KrDhondtSeatAllocationDecisionService {
    private static final String REASON = "allocated by D'Hondt highest quotients";

    public KrDhondtSeatAllocationResult allocate(KrDhondtSeatAllocationRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (request.totalSeats() < 1) {
            throw new IllegalArgumentException("totalSeats must be greater than zero");
        }
        if (request.partyVotes().isEmpty()) {
            throw new IllegalArgumentException("partyVotes must not be empty");
        }
        validateVotes(request.partyVotes());

        Map<String, Integer> seatsByPartyId = new HashMap<>();
        request.partyVotes().forEach(party -> seatsByPartyId.put(party.partyId(), 0));

        List<Quotient> quotients = new ArrayList<>();
        for (KrPartyVote partyVote : request.partyVotes()) {
            for (int divisor = 1; divisor <= request.totalSeats(); divisor++) {
                quotients.add(new Quotient(
                        partyVote.partyId(),
                        partyVote.votes(),
                        (double) partyVote.votes() / divisor));
            }
        }
        quotients.stream()
                .sorted(Comparator.comparingDouble(Quotient::value).reversed()
                        .thenComparing(Comparator.comparingInt(Quotient::votes).reversed())
                        .thenComparing(Quotient::partyId))
                .limit(request.totalSeats())
                .forEach(quotient -> seatsByPartyId.merge(quotient.partyId(), 1, Integer::sum));

        List<KrPartySeatAllocation> allocations = request.partyVotes().stream()
                .sorted(Comparator.comparing(KrPartyVote::partyId))
                .map(party -> new KrPartySeatAllocation(
                        party.partyId(),
                        party.partyName(),
                        party.votes(),
                        seatsByPartyId.getOrDefault(party.partyId(), 0)))
                .toList();
        int totalAllocated = allocations.stream().mapToInt(KrPartySeatAllocation::allocatedSeats).sum();
        return new KrDhondtSeatAllocationResult(allocations, totalAllocated, REASON);
    }

    private static void validateVotes(List<KrPartyVote> partyVotes) {
        Set<String> partyIds = new HashSet<>();
        for (KrPartyVote partyVote : partyVotes) {
            if (partyVote.votes() < 0) {
                throw new IllegalArgumentException("party votes must not be negative");
            }
            if (!partyIds.add(partyVote.partyId())) {
                throw new IllegalArgumentException("partyVotes contains duplicate party id: " + partyVote.partyId());
            }
        }
    }

    private record Quotient(String partyId, int votes, double value) {}
}
