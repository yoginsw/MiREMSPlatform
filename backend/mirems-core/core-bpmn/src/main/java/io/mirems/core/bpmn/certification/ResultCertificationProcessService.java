package io.mirems.core.bpmn.certification;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionRepository;
import io.mirems.core.domain.election.event.ElectionCertifiedEvent;
import io.mirems.core.domain.result.TabulationReport;
import io.mirems.core.domain.result.TabulationReportRepository;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({ElectionRepository.class, TabulationReportRepository.class, CertificationPdfGenerator.class})
public class ResultCertificationProcessService {
    private final ElectionRepository electionRepository;
    private final TabulationReportRepository tabulationReportRepository;
    private final CertificationPdfGenerator certificationPdfGenerator;

    public ResultCertificationProcessService(
            ElectionRepository electionRepository,
            TabulationReportRepository tabulationReportRepository,
            CertificationPdfGenerator certificationPdfGenerator) {
        this.electionRepository = Objects.requireNonNull(electionRepository, "electionRepository is required");
        this.tabulationReportRepository = Objects.requireNonNull(
                tabulationReportRepository,
                "tabulationReportRepository is required");
        this.certificationPdfGenerator = Objects.requireNonNull(
                certificationPdfGenerator,
                "certificationPdfGenerator is required");
    }

    public ResultCertificationProcessResult certify(ResultCertificationRequest request) {
        Objects.requireNonNull(request, "request is required");
        if (!request.legalReviewApproved()) {
            throw new IllegalStateException("Legal/compliance review approval is required before certification");
        }

        Election election = electionRepository
                .findById(request.electionId())
                .orElseThrow(() -> new IllegalArgumentException("election not found: " + request.electionId()));
        TabulationReport report = tabulationReportRepository
                .findById(request.tabulationReportId())
                .orElseThrow(() -> new IllegalArgumentException("tabulation report not found: " + request.tabulationReportId()));
        validateReport(election, report);

        election.certify();
        Election savedElection = electionRepository.save(election);
        ElectionCertifiedEvent event = savedElection.pullDomainEvents().stream()
                .filter(ElectionCertifiedEvent.class::isInstance)
                .map(ElectionCertifiedEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ElectionCertifiedEvent was not emitted"));
        OfficialCertificationPdf pdf = certificationPdfGenerator.generate(
                request.certificateId(),
                savedElection,
                report,
                request.electionAdminReviewer(),
                request.legalReviewer(),
                request.certifiedAt());
        return new ResultCertificationProcessResult(event, pdf);
    }

    private void validateReport(Election election, TabulationReport report) {
        if (!election.getId().equals(report.getElectionId())) {
            throw new IllegalArgumentException("tabulation report does not belong to election");
        }
        if (!report.isLocked()) {
            throw new IllegalStateException("Completed TabulationReport is required before certification");
        }
        if (!report.verifyHash()) {
            throw new IllegalStateException("TabulationReport hash verification failed");
        }
    }
}
