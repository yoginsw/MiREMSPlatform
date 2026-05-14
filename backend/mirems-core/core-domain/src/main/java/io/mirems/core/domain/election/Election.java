package io.mirems.core.domain.election;

import io.mirems.core.domain.election.event.ElectionCertifiedEvent;
import io.mirems.core.domain.election.event.ElectionClosedEvent;
import io.mirems.core.domain.election.event.ElectionCreatedEvent;
import io.mirems.core.domain.election.event.ElectionPublishedEvent;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Aggregate root for a single election event. */
public class Election {
    private final UUID id;
    private final String name;
    private final ElectionType electionType;
    private final String jurisdiction;
    private final LocalDate scheduledDate;
    private final String countryCode;
    private final String extensionPackId;
    private final List<Object> domainEvents = new ArrayList<>();

    private ElectionStatus electionStatus;

    private Election(
            UUID id,
            String name,
            ElectionType electionType,
            String jurisdiction,
            LocalDate scheduledDate,
            String countryCode,
            String extensionPackId) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = requireText(name, "name");
        this.electionType = Objects.requireNonNull(electionType, "electionType is required");
        this.jurisdiction = requireText(jurisdiction, "jurisdiction");
        this.scheduledDate = Objects.requireNonNull(scheduledDate, "scheduledDate is required");
        this.countryCode = requireCountryCode(countryCode);
        this.extensionPackId = requireText(extensionPackId, "extensionPackId");
        this.electionStatus = ElectionStatus.DRAFT;
    }

    public static Election create(
            UUID id,
            String name,
            ElectionType electionType,
            String jurisdiction,
            LocalDate scheduledDate,
            String countryCode,
            String extensionPackId) {
        Election election = new Election(id, name, electionType, jurisdiction, scheduledDate, countryCode, extensionPackId);
        election.record(new ElectionCreatedEvent(
                election.id,
                election.name,
                election.electionType,
                election.jurisdiction,
                election.scheduledDate,
                election.countryCode,
                election.extensionPackId,
                OffsetDateTime.now()));
        return election;
    }

    public void publish() {
        transitionTo(ElectionStatus.PUBLISHED);
        record(new ElectionPublishedEvent(id, OffsetDateTime.now()));
    }

    public void activate() {
        transitionTo(ElectionStatus.ACTIVE);
    }

    public void close() {
        transitionTo(ElectionStatus.CLOSED);
        record(new ElectionClosedEvent(id, OffsetDateTime.now()));
    }

    public void certify() {
        transitionTo(ElectionStatus.CERTIFIED);
        record(new ElectionCertifiedEvent(id, OffsetDateTime.now()));
    }

    public List<Object> pullDomainEvents() {
        List<Object> pendingEvents = List.copyOf(domainEvents);
        domainEvents.clear();
        return pendingEvents;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ElectionType getElectionType() {
        return electionType;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public ElectionStatus getElectionStatus() {
        return electionStatus;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getExtensionPackId() {
        return extensionPackId;
    }

    private void transitionTo(ElectionStatus targetStatus) {
        electionStatus.assertCanTransitionTo(targetStatus);
        electionStatus = targetStatus;
    }

    private void record(Object domainEvent) {
        domainEvents.add(Objects.requireNonNull(domainEvent, "domainEvent is required"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }

    private static String requireCountryCode(String value) {
        String countryCode = requireText(value, "countryCode").toUpperCase(Locale.ROOT);
        if (countryCode.length() != 2) {
            throw new IllegalArgumentException("countryCode must be an ISO 3166-1 alpha-2 code");
        }
        return countryCode;
    }
}
