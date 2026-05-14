package io.mirems.core.domain.election;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.election.event.ElectionCertifiedEvent;
import io.mirems.core.domain.election.event.ElectionClosedEvent;
import io.mirems.core.domain.election.event.ElectionCreatedEvent;
import io.mirems.core.domain.election.event.ElectionPublishedEvent;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ElectionTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4b7e-77d3-7c22-b7ec-6e89229b7d43");
    private static final LocalDate SCHEDULED_DATE = LocalDate.of(2026, 6, 3);

    @Test
    void createInitializesDraftElectionAndRecordsCreatedEvent() {
        Election election = createElection();

        assertEquals(ELECTION_ID, election.getId());
        assertEquals("2026 Local Election", election.getName());
        assertEquals(ElectionType.LOCAL, election.getElectionType());
        assertEquals("Seoul", election.getJurisdiction());
        assertEquals(SCHEDULED_DATE, election.getScheduledDate());
        assertEquals(ElectionStatus.DRAFT, election.getElectionStatus());
        assertEquals("KR", election.getCountryCode());
        assertEquals("ext-kr", election.getExtensionPackId());

        List<Object> events = election.pullDomainEvents();
        assertEquals(1, events.size());
        ElectionCreatedEvent event = assertInstanceOf(ElectionCreatedEvent.class, events.getFirst());
        assertEquals(ELECTION_ID, event.electionId());
        assertEquals("2026 Local Election", event.name());
        assertEquals(ElectionType.LOCAL, event.electionType());
        assertEquals("Seoul", event.jurisdiction());
        assertEquals(SCHEDULED_DATE, event.scheduledDate());
        assertEquals("KR", event.countryCode());
        assertEquals("ext-kr", event.extensionPackId());
        assertNotNull(event.occurredAt());
    }

    @Test
    void pullDomainEventsClearsPendingEvents() {
        Election election = createElection();

        assertEquals(1, election.pullDomainEvents().size());
        assertTrue(election.pullDomainEvents().isEmpty());
    }

    @Test
    void validLifecycleTransitionsMoveElectionForwardAndRecordEvents() {
        Election election = createElection();
        election.pullDomainEvents();

        election.publish();
        assertEquals(ElectionStatus.PUBLISHED, election.getElectionStatus());
        assertInstanceOf(ElectionPublishedEvent.class, election.pullDomainEvents().getFirst());

        election.activate();
        assertEquals(ElectionStatus.ACTIVE, election.getElectionStatus());
        assertTrue(election.pullDomainEvents().isEmpty(), "Activation is a status transition without a P1-009 event type");

        election.close();
        assertEquals(ElectionStatus.CLOSED, election.getElectionStatus());
        assertInstanceOf(ElectionClosedEvent.class, election.pullDomainEvents().getFirst());

        election.certify();
        assertEquals(ElectionStatus.CERTIFIED, election.getElectionStatus());
        assertInstanceOf(ElectionCertifiedEvent.class, election.pullDomainEvents().getFirst());
    }

    @Test
    void electionStatusAllowsOnlyConfiguredForwardTransitions() {
        for (ElectionStatus source : ElectionStatus.values()) {
            for (ElectionStatus target : ElectionStatus.values()) {
                boolean expected = switch (source) {
                    case DRAFT -> target == ElectionStatus.PUBLISHED;
                    case PUBLISHED -> target == ElectionStatus.ACTIVE;
                    case ACTIVE -> target == ElectionStatus.CLOSED;
                    case CLOSED -> target == ElectionStatus.CERTIFIED;
                    case CERTIFIED -> false;
                };

                assertEquals(expected, source.canTransitionTo(target), source + " -> " + target);
            }
        }
    }

    @Test
    void invalidTransitionThrowsStableDomainException() {
        Election election = createElection();
        election.pullDomainEvents();

        InvalidElectionStateException exception = assertThrows(InvalidElectionStateException.class, election::close);

        assertEquals("MIR-ELECTION-STATE-001", exception.getErrorCode());
        assertEquals("Domain rule violation", exception.getTitle());
        assertTrue(exception.getMessage().contains("DRAFT"));
        assertTrue(exception.getMessage().contains("CLOSED"));
        assertEquals(ElectionStatus.DRAFT, election.getElectionStatus());
        assertTrue(election.pullDomainEvents().isEmpty());
    }

    @Test
    void certifiedElectionCannotTransitionFurther() {
        Election election = createElection();
        election.publish();
        election.activate();
        election.close();
        election.certify();
        election.pullDomainEvents();

        InvalidElectionStateException exception = assertThrows(InvalidElectionStateException.class, election::publish);

        assertTrue(exception.getMessage().contains("CERTIFIED"));
        assertEquals(ElectionStatus.CERTIFIED, election.getElectionStatus());
        assertTrue(election.pullDomainEvents().isEmpty());
    }

    @Test
    void createRejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> Election.create(
                ELECTION_ID,
                " ",
                ElectionType.LOCAL,
                "Seoul",
                SCHEDULED_DATE,
                "KR",
                "ext-kr"));

        assertThrows(IllegalArgumentException.class, () -> Election.create(
                ELECTION_ID,
                "2026 Local Election",
                ElectionType.LOCAL,
                "",
                SCHEDULED_DATE,
                "KR",
                "ext-kr"));

        assertThrows(IllegalArgumentException.class, () -> Election.create(
                ELECTION_ID,
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                SCHEDULED_DATE,
                "K",
                "ext-kr"));
    }

    @Test
    void createRejectsNullRequiredFields() {
        assertThrows(NullPointerException.class, () -> Election.create(
                null,
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                SCHEDULED_DATE,
                "KR",
                "ext-kr"));

        assertThrows(IllegalArgumentException.class, () -> Election.create(
                ELECTION_ID,
                null,
                ElectionType.LOCAL,
                "Seoul",
                SCHEDULED_DATE,
                "KR",
                "ext-kr"));
    }

    @Test
    void domainEventsExposeOccurredAtTimestamps() {
        Election election = createElection();
        OffsetDateTime createdAt = ((ElectionCreatedEvent) election.pullDomainEvents().getFirst()).occurredAt();

        election.publish();
        OffsetDateTime publishedAt = ((ElectionPublishedEvent) election.pullDomainEvents().getFirst()).occurredAt();

        assertFalse(publishedAt.isBefore(createdAt));
    }

    private static Election createElection() {
        return Election.create(
                ELECTION_ID,
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                SCHEDULED_DATE,
                "KR",
                "ext-kr");
    }
}
