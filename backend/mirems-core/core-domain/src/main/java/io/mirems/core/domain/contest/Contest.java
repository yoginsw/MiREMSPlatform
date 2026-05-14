package io.mirems.core.domain.contest;

import io.mirems.core.domain.election.Election;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Entity representing a single race or question within an election. */
public class Contest {
    private final UUID id;
    private final Election election;
    private final ContestType contestType;
    private final String name;
    private final int seats;
    private final int voteLimit;
    private final List<Candidate> candidates = new ArrayList<>();

    private Contest(UUID id, Election election, ContestType contestType, String name, int seats, int voteLimit) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.election = Objects.requireNonNull(election, "election is required");
        this.contestType = Objects.requireNonNull(contestType, "contestType is required");
        this.name = requireText(name, "name");
        validateSeatConfiguration(seats, voteLimit);
        this.seats = seats;
        this.voteLimit = voteLimit;
    }

    public static Contest create(
            UUID id,
            Election election,
            ContestType contestType,
            String name,
            int seats,
            int voteLimit) {
        return new Contest(id, election, contestType, name, seats, voteLimit);
    }

    public Candidate addCandidate(UUID id, String name, String partyAffiliation) {
        Candidate candidate = Candidate.create(id, this, name, partyAffiliation);
        candidates.add(candidate);
        return candidate;
    }

    public UUID getId() {
        return id;
    }

    public Election getElection() {
        return election;
    }

    public ContestType getContestType() {
        return contestType;
    }

    public String getName() {
        return name;
    }

    public int getSeats() {
        return seats;
    }

    public int getVoteLimit() {
        return voteLimit;
    }

    public List<Candidate> getCandidates() {
        return List.copyOf(candidates);
    }

    private static void validateSeatConfiguration(int seats, int voteLimit) {
        if (seats < 1) {
            throw new ContestValidationException("seats must be greater than zero");
        }
        if (voteLimit < 1) {
            throw new ContestValidationException("voteLimit must be greater than zero");
        }
        if (voteLimit > seats) {
            throw new ContestValidationException("voteLimit must be less than or equal to seats");
        }
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
