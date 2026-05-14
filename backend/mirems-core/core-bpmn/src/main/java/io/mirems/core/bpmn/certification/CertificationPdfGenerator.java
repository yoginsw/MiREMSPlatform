package io.mirems.core.bpmn.certification;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.result.TabulationReport;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface CertificationPdfGenerator {
    OfficialCertificationPdf generate(
            UUID certificateId,
            Election election,
            TabulationReport report,
            String electionAdminReviewer,
            String legalReviewer,
            OffsetDateTime certifiedAt);
}
