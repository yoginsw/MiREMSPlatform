package io.mirems.core.domain.ballot;

import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.election.Election;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Entity representing the versioned ballot definition for an election. */
@Entity
@Table(name = "ballots")
public class Ballot {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @Transient
    private final List<BallotContest> ballotContests = new ArrayList<>();

    @OneToMany(mappedBy = "ballot", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<BallotStyle> ballotStyles = new ArrayList<>();

    @Column(name = "ballot_version", nullable = false)
    private int ballotVersion;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Ballot() {
        // JPA constructor.
    }

    private Ballot(UUID id, Election election) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.election = Objects.requireNonNull(election, "election is required");
        this.ballotVersion = 1;
        this.active = false;
    }

    public static Ballot create(UUID id, Election election) {
        return new Ballot(id, election);
    }

    public BallotContest addContest(Contest contest, int displayOrder, String presentationTitle) {
        BallotContest ballotContest = BallotContest.create(this, contest, displayOrder, presentationTitle);
        ballotContests.add(ballotContest);
        incrementVersion();
        return ballotContest;
    }

    public BallotStyle addStyle(
            UUID id,
            String styleCode,
            String district,
            String language,
            Set<AccessibilityFeature> accessibilityFeatures) {
        BallotStyle ballotStyle = BallotStyle.create(id, this, styleCode, district, language, accessibilityFeatures);
        ballotStyles.add(ballotStyle);
        incrementVersion();
        return ballotStyle;
    }

    public void activate() {
        ensureHasContest();
        if (!active) {
            active = true;
            incrementVersion();
        }
    }

    public void deactivate() {
        if (active) {
            active = false;
            incrementVersion();
        }
    }

    public UUID getId() {
        return id;
    }

    public Election getElection() {
        return election;
    }

    public int getBallotVersion() {
        return ballotVersion;
    }

    public boolean isActive() {
        return active;
    }

    public List<BallotContest> getBallotContests() {
        return List.copyOf(ballotContests);
    }

    public List<BallotStyle> getBallotStyles() {
        return List.copyOf(ballotStyles);
    }

    private void ensureHasContest() {
        if (ballotContests.isEmpty()) {
            throw new BallotValidationException("A Ballot must have at least one Contest before activation");
        }
    }

    private void incrementVersion() {
        ballotVersion++;
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }
}
