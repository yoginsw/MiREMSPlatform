package io.mirems.core.domain.election;

import io.mirems.core.domain.election.event.ElectionCertifiedEvent;
import io.mirems.core.domain.election.event.ElectionClosedEvent;
import io.mirems.core.domain.election.event.ElectionCreatedEvent;
import io.mirems.core.domain.election.event.ElectionPublishedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Aggregate root for a single election event. */
@Entity
@Table(name = "elections")
public class Election {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "election_type", nullable = false)
    private ElectionType electionType;

    @Column(name = "jurisdiction", nullable = false)
    private String jurisdiction;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "extension_pack_id", nullable = false)
    private String extensionPackId;

    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "election_status", nullable = false)
    private ElectionStatus electionStatus;

    protected Election() {
        // JPA constructor.
    }

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
