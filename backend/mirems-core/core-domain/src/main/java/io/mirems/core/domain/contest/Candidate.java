package io.mirems.core.domain.contest;

import io.mirems.core.domain.contest.event.CandidateApprovedEvent;
import io.mirems.core.domain.contest.event.CandidateDisqualifiedEvent;
import io.mirems.core.domain.contest.event.CandidateWithdrawnEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Entity representing a candidate or option in a contest. */
@Entity
@Table(name = "candidates")
public class Candidate {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "party_affiliation", nullable = false)
    private String partyAffiliation;

    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_status", nullable = false)
    private CandidateStatus candidateStatus;

    protected Candidate() {
        // JPA constructor.
    }

    private Candidate(UUID id, Contest contest, String name, String partyAffiliation) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.contest = Objects.requireNonNull(contest, "contest is required");
        this.name = Contest.requireText(name, "name");
        this.partyAffiliation = Contest.requireText(partyAffiliation, "partyAffiliation");
        this.candidateStatus = CandidateStatus.PENDING;
    }

    static Candidate create(UUID id, Contest contest, String name, String partyAffiliation) {
        return new Candidate(id, contest, name, partyAffiliation);
    }

    public void approve() {
        transitionTo(CandidateStatus.APPROVED);
        record(new CandidateApprovedEvent(
                id,
                contest.getId(),
                contest.getElection().getId(),
                OffsetDateTime.now()));
    }

    public void withdraw() {
        transitionTo(CandidateStatus.WITHDRAWN);
        record(new CandidateWithdrawnEvent(
                id,
                contest.getId(),
                contest.getElection().getId(),
                OffsetDateTime.now()));
    }

    public void disqualify() {
        disqualify(List.of());
    }

    public void disqualify(List<String> reasons) {
        transitionTo(CandidateStatus.DISQUALIFIED);
        record(new CandidateDisqualifiedEvent(
                id,
                contest.getId(),
                contest.getElection().getId(),
                reasons,
                OffsetDateTime.now()));
    }

    public List<Object> pullDomainEvents() {
        List<Object> pendingEvents = List.copyOf(domainEvents);
        domainEvents.clear();
        return pendingEvents;
    }

    public UUID getId() {
        return id;
    }

    public Contest getContest() {
        return contest;
    }

    public String getName() {
        return name;
    }

    public String getPartyAffiliation() {
        return partyAffiliation;
    }

    public CandidateStatus getCandidateStatus() {
        return candidateStatus;
    }

    private void transitionTo(CandidateStatus targetStatus) {
        candidateStatus.assertCanTransitionTo(targetStatus);
        candidateStatus = targetStatus;
    }

    private void record(Object domainEvent) {
        domainEvents.add(Objects.requireNonNull(domainEvent, "domainEvent is required"));
    }
}
