package io.mirems.core.bpmn.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

class AdvancedAuditProcessServiceTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4be0-aaaa-7bbb-8ccc-233344445555");
    private static final UUID REPORT_ID = UUID.fromString("018f4be0-bbbb-7ccc-8ddd-344455556666");
    private static final OffsetDateTime GENERATED_AT = OffsetDateTime.parse("2026-06-06T10:00:00Z");

    @Test
    void generatesRiskLimitingAuditPlanAndDeterministicStatisticalSample() {
        AdvancedAuditProcessService service = new AdvancedAuditProcessService(repositoryWithCompleteTrail(), allowAuditor());
        AdvancedAuditRequest request = new AdvancedAuditRequest(
                REPORT_ID,
                ELECTION_ID,
                "auditor-1",
                GENERATED_AT,
                10_000,
                500,
                0.10,
                0.05,
                42L,
                ballotPopulation(500));

        AdvancedAuditReport report = service.generate(request);

        assertThat(report.riskLimitingAuditPlan().riskLimit()).isEqualTo(0.05);
        assertThat(report.riskLimitingAuditPlan().margin()).isEqualTo(0.10);
        assertThat(report.riskLimitingAuditPlan().recommendedSampleSize()).isEqualTo(300);
        assertThat(report.statisticalSample().seed()).isEqualTo(42L);
        assertThat(report.statisticalSample().sampledBallotIds()).containsExactlyElementsOf(expectedSample(request.ballotPopulation(), 42L, 300));
        assertThat(report.statisticalSample().sampleSize()).isEqualTo(300);
        AdvancedAuditReport differentSeedReport = service.generate(new AdvancedAuditRequest(
                REPORT_ID,
                ELECTION_ID,
                "auditor-1",
                GENERATED_AT,
                10_000,
                500,
                0.10,
                0.05,
                99L,
                request.ballotPopulation()));
        assertThat(differentSeedReport.statisticalSample().sampledBallotIds())
                .isNotEqualTo(report.statisticalSample().sampledBallotIds());
    }

    @Test
    void createsChronologicalChainOfCustodyAndJsonXmlExportsWithoutPiiPayloads() {
        AdvancedAuditProcessService service = new AdvancedAuditProcessService(repositoryWithCompleteTrail(), allowAuditor());
        AdvancedAuditRequest request = defaultRequest();

        AdvancedAuditReport report = service.generate(request);

        assertThat(report.chainOfCustodyReport().entries())
                .extracting(ChainOfCustodyEntry::eventType)
                .containsExactly("ElectionCreated", "VotingSessionOpened", "VoteCast", "TabulationCompleted", "ResultCertified");
        assertThat(report.chainOfCustodyReport().entries())
                .allSatisfy(entry -> assertThat(entry.payloadKeys()).doesNotContain("voterName", "address", "ssn"));
        assertThat(report.jsonExport().contentType()).isEqualTo("application/vnd.mirems.audit+json");
        assertThat(report.xmlExport().contentType()).isEqualTo("application/vnd.mirems.audit+xml");
        String json = new String(report.jsonExport().content(), StandardCharsets.UTF_8);
        String xml = new String(report.xmlExport().content(), StandardCharsets.UTF_8);
        assertThat(json).contains("\"vvsgComplete\":true").contains("\"chainOfCustody\"").doesNotContain("Ada Lovelace");
        assertThat(xml).contains("<vvsgComplete>true</vvsgComplete>").contains("<chainOfCustody>").doesNotContain("Ada Lovelace");
    }

    @Test
    void verifiesVvsgAuditTrailCompletenessAndReportsMissingEventTypes() {
        AdvancedAuditProcessService service = new AdvancedAuditProcessService(repositoryWithEvents(List.of(
                event("ElectionCreated", "2026-06-01T09:00:00Z", Map.of("status", "CREATED")),
                event("VoteCast", "2026-06-02T09:00:00Z", Map.of("receipt", "R-1")))), allowAuditor());

        AdvancedAuditReport report = service.generate(defaultRequest());

        assertThat(report.vvsgVerification().complete()).isFalse();
        assertThat(report.vvsgVerification().missingEventTypes()).containsExactly("VotingSessionOpened", "TabulationCompleted", "ResultCertified");
        assertThat(report.jsonExport().fileName()).isEqualTo("advanced-audit-" + ELECTION_ID + ".json");
        assertThat(report.xmlExport().fileName()).isEqualTo("advanced-audit-" + ELECTION_ID + ".xml");
    }

    @Test
    void rejectsNonAuditorInvalidRiskParametersAndEmptyPopulation() {
        AdvancedAuditProcessService service = new AdvancedAuditProcessService(repositoryWithCompleteTrail(), denyAuditor());
        assertThatThrownBy(() -> service.generate(new AdvancedAuditRequest(
                        REPORT_ID, ELECTION_ID, "admin-1", GENERATED_AT, 100, 10, 0.10, 0.05, 7L, List.of("b-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AUDITOR");
        AdvancedAuditProcessService authorizedService = new AdvancedAuditProcessService(repositoryWithCompleteTrail(), allowAuditor());
        assertThatThrownBy(() -> authorizedService.generate(new AdvancedAuditRequest(
                        REPORT_ID, ELECTION_ID, "auditor-1", GENERATED_AT, 100, 10, 0.0, 0.05, 7L, List.of("b-1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("margin");
        assertThatThrownBy(() -> authorizedService.generate(new AdvancedAuditRequest(
                        REPORT_ID, ELECTION_ID, "auditor-1", GENERATED_AT, 100, 10, 0.10, 0.05, 7L, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ballotPopulation");
    }

    private static List<String> ballotPopulation(int size) {
        return java.util.stream.IntStream.rangeClosed(1, size)
                .mapToObj(index -> String.format("ballot-%03d", index))
                .toList();
    }

    private static AdvancedAuditAuthorizer allowAuditor() {
        return (actorId, electionId) -> { };
    }

    private static AdvancedAuditAuthorizer denyAuditor() {
        return (actorId, electionId) -> { throw new IllegalArgumentException("AUDITOR authority is required"); };
    }

    private static List<String> expectedSample(List<String> population, long seed, int sampleSize) {
        ArrayList<String> shuffled = new ArrayList<>(population);
        java.util.Collections.shuffle(shuffled, new java.util.Random(seed));
        return shuffled.subList(0, sampleSize).stream().sorted().toList();
    }

    private static AdvancedAuditRequest defaultRequest() {
        return new AdvancedAuditRequest(
                REPORT_ID,
                ELECTION_ID,
                "auditor-1",
                GENERATED_AT,
                10_000,
                500,
                0.10,
                0.05,
                42L,
                ballotPopulation(500));
    }

    private static InMemoryRepository repositoryWithCompleteTrail() {
        return repositoryWithEvents(List.of(
                event("VoteCast", "2026-06-02T09:00:00Z", Map.of("receipt", "R-1", "voterName", "Ada Lovelace")),
                event("ElectionCreated", "2026-06-01T09:00:00Z", Map.of("status", "CREATED")),
                event("TabulationCompleted", "2026-06-03T20:00:00Z", Map.of("reportHash", "abc123")),
                event("VotingSessionOpened", "2026-06-02T08:55:00Z", Map.of("method", "ELECTION_DAY")),
                event("ResultCertified", "2026-06-04T12:00:00Z", Map.of("certificate", "CERT-1"))));
    }

    private static InMemoryRepository repositoryWithEvents(List<AuditEvent> events) {
        return new InMemoryRepository(events);
    }

    private static AuditEvent event(String type, String occurredAt, Map<String, Object> payload) {
        return AuditEvent.create(
                UUID.nameUUIDFromBytes((type + occurredAt).getBytes(StandardCharsets.UTF_8)),
                type,
                ELECTION_ID,
                "Election",
                payload,
                "system",
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

        @Override
        public List<AuditEvent> findAllChronologically() {
            return events.stream().sorted(java.util.Comparator.comparing(AuditEvent::getOccurredAt).thenComparing(AuditEvent::getId)).toList();
        }
    }
}
