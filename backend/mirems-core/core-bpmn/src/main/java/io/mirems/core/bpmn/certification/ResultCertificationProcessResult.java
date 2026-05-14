package io.mirems.core.bpmn.certification;

import io.mirems.core.domain.election.event.ElectionCertifiedEvent;
import java.util.Objects;

public record ResultCertificationProcessResult(ElectionCertifiedEvent event, OfficialCertificationPdf pdf) {
    public ResultCertificationProcessResult {
        Objects.requireNonNull(event, "event is required");
        Objects.requireNonNull(pdf, "pdf is required");
    }
}
