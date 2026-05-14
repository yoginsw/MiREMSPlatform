package io.mirems.core.bpmn.certification;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.result.TabulationReport;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SimpleCertificationPdfGenerator implements CertificationPdfGenerator {
    @Override
    public OfficialCertificationPdf generate(
            UUID certificateId,
            Election election,
            TabulationReport report,
            String electionAdminReviewer,
            String legalReviewer,
            OffsetDateTime certifiedAt) {
        Objects.requireNonNull(election, "election is required");
        Objects.requireNonNull(report, "report is required");
        String body = "MiREMS Official Election Certification Report\n"
                + "Election: " + election.getName() + "\n"
                + "Election ID: " + election.getId() + "\n"
                + "Tabulation Report ID: " + report.getId() + "\n"
                + "Tabulation Hash: " + report.getHash() + "\n"
                + "Total Ballots Counted: " + report.totalBallotsCounted() + "\n"
                + "Election Admin Reviewer: " + electionAdminReviewer + "\n"
                + "Legal Reviewer: " + legalReviewer + "\n"
                + "Certified At: " + certifiedAt + "\n";
        byte[] pdfBytes = minimalPdf(body);
        return new OfficialCertificationPdf(
                certificateId,
                election.getId(),
                report.getId(),
                "official-certification-" + election.getId() + ".pdf",
                "application/pdf",
                pdfBytes,
                certifiedAt);
    }

    private byte[] minimalPdf(String body) {
        String escaped = body.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace("\n", "\\n");
        String pdf = "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n"
                + "4 0 obj << /Length " + (escaped.length() + 47) + " >> stream\n"
                + "BT /F1 10 Tf 50 740 Td (" + escaped + ") Tj ET\n"
                + "endstream endobj\n"
                + "xref\n0 5\n0000000000 65535 f \n"
                + "trailer << /Root 1 0 R /Size 5 >>\nstartxref\n0\n%%EOF\n";
        return pdf.getBytes(StandardCharsets.UTF_8);
    }
}
