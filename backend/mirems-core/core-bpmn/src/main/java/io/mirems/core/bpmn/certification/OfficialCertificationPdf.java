package io.mirems.core.bpmn.certification;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record OfficialCertificationPdf(
        UUID certificateId,
        UUID electionId,
        UUID tabulationReportId,
        String fileName,
        String contentType,
        byte[] content,
        OffsetDateTime generatedAt) {
    public OfficialCertificationPdf {
        Objects.requireNonNull(certificateId, "certificateId is required");
        Objects.requireNonNull(electionId, "electionId is required");
        Objects.requireNonNull(tabulationReportId, "tabulationReportId is required");
        requireText(fileName, "fileName");
        requireText(contentType, "contentType");
        content = Arrays.copyOf(Objects.requireNonNull(content, "content is required"), content.length);
        if (content.length == 0) {
            throw new IllegalArgumentException("content must not be empty");
        }
        Objects.requireNonNull(generatedAt, "generatedAt is required");
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
