package io.mirems.core.bpmn.audit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditReviewProcessServiceTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4bd0-aaaa-7bbb-8ccc-233344445555");
    private static final UUID REPORT_ID = UUID.fromString("018f4bd0-bbbb-7ccc-8ddd-344455556666");
    private static final OffsetDateTime GENERATED_AT = OffsetDateTime.parse("2026-06-05T10:00:00Z");

    @Test
    void auditorCanGenerateJsonAndPdfReportFromChronologicalAuditEvents() {
        InMemoryRepository repository = new InMemoryRepository(List.of(
                auditEvent("018f4bd0-0002-7000-8000-000000000002", "ElectionClosed", "admin-2", "2026-06-03T20:00:00Z", Map.of("status", "CLOSED")),
                auditEvent("018f4bd0-0001-7000-8000-000000000001", "ElectionPublished", "admin-1", "2026-06-01T09:00:00Z", Map.of("status", "PUBLISHED"))));
        AuditReviewProcessService service = new AuditReviewProcessService(repository, new SimpleAuditReportGenerator());
        AuditReviewRequest request = new AuditReviewRequest(REPORT_ID, ELECTION_ID, "auditor-1", "AUDITOR", GENERATED_AT);

        AuditReviewProcessResult result = service.generate(request);

        AuditReport report = result.report();
        assertEquals(REPORT_ID, report.reportId());
        assertEquals(ELECTION_ID, report.electionId());
        assertEquals("auditor-1", report.generatedBy());
        assertEquals(GENERATED_AT, report.generatedAt());
        assertEquals(2, report.eventCount());
        assertEquals("application/json", report.jsonContentType());
        assertEquals("application/pdf", report.pdfContentType());
        String json = new String(report.jsonContent(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"reportId\":\"" + REPORT_ID + "\""));
        assertTrue(json.contains("ElectionPublished"));
        assertTrue(json.indexOf("ElectionPublished") < json.indexOf("ElectionClosed"));
        String pdf = new String(report.pdfContent(), StandardCharsets.UTF_8);
        assertTrue(pdf.startsWith("%PDF-1.4"));
        assertTrue(pdf.contains("MiREMS Post-Election Audit Report"));
    }

    @Test
    void nonAuditorCannotInitiateAuditReview() {
        AuditReviewProcessService service = new AuditReviewProcessService(new InMemoryRepository(List.of()), new SimpleAuditReportGenerator());
        AuditReviewRequest request = new AuditReviewRequest(REPORT_ID, ELECTION_ID, "admin-1", "ELECTION_ADMIN", GENERATED_AT);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.generate(request));

        assertEquals("AUDITOR role is required to initiate audit review", exception.getMessage());
    }

    @Test
    void reportContentUsesDefensiveCopies() {
        byte[] json = "{}".getBytes(StandardCharsets.UTF_8);
        byte[] pdf = "%PDF".getBytes(StandardCharsets.UTF_8);

        AuditReport report = new AuditReport(
                REPORT_ID,
                ELECTION_ID,
                "auditor-1",
                GENERATED_AT,
                0,
                "audit-report-" + ELECTION_ID + ".json",
                "application/json",
                json,
                "audit-report-" + ELECTION_ID + ".pdf",
                "application/pdf",
                pdf);

        json[0] = '[';
        pdf[0] = '!';

        assertArrayEquals("{}".getBytes(StandardCharsets.UTF_8), report.jsonContent());
        assertArrayEquals("%PDF".getBytes(StandardCharsets.UTF_8), report.pdfContent());
    }

    private static AuditEvent auditEvent(String id, String type, String actor, String occurredAt, Map<String, Object> payload) {
        return AuditEvent.create(
                UUID.fromString(id),
                type,
                ELECTION_ID,
                "Election",
                payload,
                actor,
                OffsetDateTime.parse(occurredAt),
                "127.0.0.1");
    }

    private static final class InMemoryRepository implements AuditEventRepository {
        private final List<AuditEvent> events;

        private InMemoryRepository(List<AuditEvent> events) {
            this.events = new ArrayList<>(events);
        }

        @Override
        public AuditEvent save(AuditEvent auditEvent) {
            events.add(auditEvent);
            return auditEvent;
        }

        @Override
        public Optional<AuditEvent> findById(UUID id) {
            return events.stream().filter(event -> event.getId().equals(id)).findFirst();
        }

        @Override
        public List<AuditEvent> findByAggregateId(UUID aggregateId) {
            return events.stream().filter(event -> event.getAggregateId().equals(aggregateId)).toList();
        }

        @Override
        public List<AuditEvent> findByEventType(String eventType) {
            return events.stream().filter(event -> event.getEventType().equals(eventType)).toList();
        }
    }
}
