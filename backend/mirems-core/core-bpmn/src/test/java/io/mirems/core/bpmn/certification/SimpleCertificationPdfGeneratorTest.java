package io.mirems.core.bpmn.certification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.result.ContestTally;
import io.mirems.core.domain.result.TabulationReport;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SimpleCertificationPdfGeneratorTest {
    @Test
    void generatesDefensivePdfPayloadWithOfficialCertificationMetadata() {
        UUID electionId = UUID.fromString("018f4bd0-aaaa-7bbb-8ccc-233344445555");
        UUID reportId = UUID.fromString("018f4bd0-bbbb-7ccc-8ddd-344455556666");
        UUID certificateId = UUID.fromString("018f4bd0-cccc-7ddd-8eee-455566667777");
        UUID contestId = UUID.fromString("018f4bd0-dddd-7eee-8fff-566677778888");
        UUID candidateId = UUID.fromString("018f4bd0-eeee-7fff-8000-677788889999");
        OffsetDateTime certifiedAt = OffsetDateTime.parse("2026-06-04T10:00:00Z");
        Election election = Election.create(
                electionId,
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                LocalDate.of(2026, 6, 3),
                "KR",
                "ext-kr");
        TabulationReport report = TabulationReport.draft(
                reportId,
                electionId,
                Map.of(contestId, new ContestTally(contestId, Map.of(candidateId, 100), 100)),
                OffsetDateTime.parse("2026-06-03T23:30:00Z"));
        report.lock(OffsetDateTime.parse("2026-06-04T00:30:00Z"));

        OfficialCertificationPdf pdf = new SimpleCertificationPdfGenerator()
                .generate(certificateId, election, report, "election-admin-1", "legal-reviewer-1", certifiedAt);

        assertEquals(certificateId, pdf.certificateId());
        assertEquals("application/pdf", pdf.contentType());
        assertEquals("official-certification-" + electionId + ".pdf", pdf.fileName());
        assertTrue(new String(pdf.content(), StandardCharsets.UTF_8).startsWith("%PDF-1.4"));
        assertTrue(new String(pdf.content(), StandardCharsets.UTF_8).contains("MiREMS Official Election Certification Report"));
        assertNotSame(pdf.content(), pdf.content());
    }
}
