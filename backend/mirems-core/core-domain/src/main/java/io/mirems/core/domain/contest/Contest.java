package io.mirems.core.domain.contest;

import io.mirems.core.domain.election.Election;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Entity representing a single race or question within an election. */
@Entity
@Table(name = "contests")
public class Contest {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @Enumerated(EnumType.STRING)
    @Column(name = "contest_type", nullable = false)
    private ContestType contestType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "seats", nullable = false)
    private int seats;

    @Column(name = "vote_limit", nullable = false)
    private int voteLimit;

    @OneToMany(mappedBy = "contest", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Candidate> candidates = new ArrayList<>();

    protected Contest() {
        // JPA constructor.
    }

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
