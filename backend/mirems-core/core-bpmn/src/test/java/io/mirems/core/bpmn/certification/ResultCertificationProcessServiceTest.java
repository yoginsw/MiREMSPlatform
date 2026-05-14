package io.mirems.core.bpmn.certification;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionRepository;
import io.mirems.core.domain.election.ElectionStatus;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.election.event.ElectionCertifiedEvent;
import io.mirems.core.domain.result.ContestTally;
import io.mirems.core.domain.result.TabulationReport;
import io.mirems.core.domain.result.TabulationReportRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResultCertificationProcessServiceTest {
    private static final UUID ELECTION_ID = UUID.fromString("018f4bd0-aaaa-7bbb-8ccc-233344445555");
    private static final UUID REPORT_ID = UUID.fromString("018f4bd0-bbbb-7ccc-8ddd-344455556666");
    private static final UUID CERTIFICATE_ID = UUID.fromString("018f4bd0-cccc-7ddd-8eee-455566667777");
    private static final UUID CONTEST_ID = UUID.fromString("018f4bd0-dddd-7eee-8fff-566677778888");
    private static final UUID CANDIDATE_ID = UUID.fromString("018f4bd0-eeee-7fff-8000-677788889999");
    private static final OffsetDateTime GENERATED_AT = OffsetDateTime.parse("2026-06-03T23:30:00Z");
    private static final OffsetDateTime CERTIFIED_AT = OffsetDateTime.parse("2026-06-04T10:00:00Z");

    @Test
    void certifiesClosedElectionFromCompletedTabulationReportAndGeneratesOfficialPdf() {
        Election election = closedElection();
        TabulationReport report = completedReport();
        InMemoryElectionRepository elections = new InMemoryElectionRepository(election);
        InMemoryTabulationReportRepository reports = new InMemoryTabulationReportRepository(report);
        StubCertificationPdfGenerator pdfGenerator = new StubCertificationPdfGenerator();
        ResultCertificationProcessService service = new ResultCertificationProcessService(elections, reports, pdfGenerator);

        ResultCertificationProcessResult result = service.certify(new ResultCertificationRequest(
                CERTIFICATE_ID,
                ELECTION_ID,
                REPORT_ID,
                "election-admin-1",
                "legal-reviewer-1",
                true,
                CERTIFIED_AT));

        assertEquals(ElectionStatus.CERTIFIED, election.getElectionStatus());
        assertEquals(1, elections.savedElections.size());
        assertEquals(ELECTION_ID, result.event().electionId());
        assertTrue(result.event() instanceof ElectionCertifiedEvent);
        assertEquals(CERTIFICATE_ID, result.pdf().certificateId());
        assertEquals(ELECTION_ID, result.pdf().electionId());
        assertEquals(REPORT_ID, result.pdf().tabulationReportId());
        assertArrayEquals(StubCertificationPdfGenerator.PDF_BYTES, result.pdf().content());
        assertEquals("application/pdf", result.pdf().contentType());
        assertEquals("official-certification-018f4bd0-aaaa-7bbb-8ccc-233344445555.pdf", result.pdf().fileName());
        assertEquals(1, pdfGenerator.generatedReports.size());
    }

    @Test
    void rejectsCertificationWhenLegalReviewIsRequiredButNotApproved() {
        ResultCertificationProcessService service = new ResultCertificationProcessService(
                new InMemoryElectionRepository(closedElection()),
                new InMemoryTabulationReportRepository(completedReport()),
                new StubCertificationPdfGenerator());

        ResultCertificationRequest request = new ResultCertificationRequest(
                CERTIFICATE_ID,
                ELECTION_ID,
                REPORT_ID,
                "election-admin-1",
                "legal-reviewer-1",
                false,
                CERTIFIED_AT);

        assertThrows(IllegalStateException.class, () -> service.certify(request));
    }

    @Test
    void rejectsCertificationWhenTabulationReportIsNotLocked() {
        ResultCertificationProcessService service = new ResultCertificationProcessService(
                new InMemoryElectionRepository(closedElection()),
                new InMemoryTabulationReportRepository(unlockedReport()),
                new StubCertificationPdfGenerator());

        ResultCertificationRequest request = new ResultCertificationRequest(
                CERTIFICATE_ID,
                ELECTION_ID,
                REPORT_ID,
                "election-admin-1",
                "legal-reviewer-1",
                true,
                CERTIFIED_AT);

        assertThrows(IllegalStateException.class, () -> service.certify(request));
    }

    private static final class InMemoryElectionRepository implements ElectionRepository {
        private final Election election;
        private final List<Election> savedElections = new ArrayList<>();

        private InMemoryElectionRepository(Election election) {
            this.election = election;
        }

        @Override
        public Election save(Election election) {
            savedElections.add(election);
            return election;
        }

        @Override
        public Optional<Election> findById(UUID id) {
            return ELECTION_ID.equals(id) ? Optional.of(election) : Optional.empty();
        }
    }

    private static final class InMemoryTabulationReportRepository implements TabulationReportRepository {
        private final TabulationReport report;

        private InMemoryTabulationReportRepository(TabulationReport report) {
            this.report = report;
        }

        @Override
        public TabulationReport save(TabulationReport report) {
            return report;
        }

        @Override
        public Optional<TabulationReport> findById(UUID id) {
            return REPORT_ID.equals(id) ? Optional.of(report) : Optional.empty();
        }

        @Override
        public Optional<TabulationReport> findByElectionId(UUID electionId) {
            return ELECTION_ID.equals(electionId) ? Optional.of(report) : Optional.empty();
        }
    }

    private static final class StubCertificationPdfGenerator implements CertificationPdfGenerator {
        private static final byte[] PDF_BYTES = "%PDF-1.4\n% official certification\n".getBytes();
        private final List<TabulationReport> generatedReports = new ArrayList<>();

        @Override
        public OfficialCertificationPdf generate(
                UUID certificateId,
                Election election,
                TabulationReport report,
                String electionAdminReviewer,
                String legalReviewer,
                OffsetDateTime certifiedAt) {
            generatedReports.add(report);
            return new OfficialCertificationPdf(
                    certificateId,
                    election.getId(),
                    report.getId(),
                    "official-certification-" + election.getId() + ".pdf",
                    "application/pdf",
                    PDF_BYTES,
                    certifiedAt);
        }
    }

    private static Election closedElection() {
        Election election = Election.create(
                ELECTION_ID,
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                LocalDate.of(2026, 6, 3),
                "KR",
                "ext-kr");
        election.pullDomainEvents();
        election.publish();
        election.activate();
        election.close();
        election.pullDomainEvents();
        return election;
    }

    private static TabulationReport completedReport() {
        TabulationReport report = unlockedReport();
        report.lock(GENERATED_AT.plusHours(1));
        return report;
    }

    private static TabulationReport unlockedReport() {
        return TabulationReport.draft(
                REPORT_ID,
                ELECTION_ID,
                Map.of(CONTEST_ID, new ContestTally(CONTEST_ID, Map.of(CANDIDATE_ID, 100), 100)),
                GENERATED_AT);
    }
}
