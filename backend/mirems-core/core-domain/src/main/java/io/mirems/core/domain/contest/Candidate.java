package io.mirems.core.domain.contest;

import io.mirems.core.domain.contest.event.CandidateApprovedEvent;
import io.mirems.core.domain.contest.event.CandidateWithdrawnEvent;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Entity representing a candidate or option in a contest. */
public class Candidate {
    private final UUID id;
    private final Contest contest;
    private final String name;
    private final String partyAffiliation;
    private final List<Object> domainEvents = new ArrayList<>();

    private CandidateStatus candidateStatus;

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
        transitionTo(CandidateStatus.DISQUALIFIED);
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
