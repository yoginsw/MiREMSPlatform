package io.mirems.core.bpmn.audit;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record AuditReport(
        UUID reportId,
        UUID electionId,
        String generatedBy,
        OffsetDateTime generatedAt,
        int eventCount,
        String jsonFileName,
        String jsonContentType,
        byte[] jsonContent,
        String pdfFileName,
        String pdfContentType,
        byte[] pdfContent) {
    public AuditReport {
        Objects.requireNonNull(reportId, "reportId is required");
        Objects.requireNonNull(electionId, "electionId is required");
        requireText(generatedBy, "generatedBy");
        Objects.requireNonNull(generatedAt, "generatedAt is required");
        if (eventCount < 0) {
            throw new IllegalArgumentException("eventCount must not be negative");
        }
        requireText(jsonFileName, "jsonFileName");
        requireText(jsonContentType, "jsonContentType");
        jsonContent = Arrays.copyOf(Objects.requireNonNull(jsonContent, "jsonContent is required"), jsonContent.length);
        requireText(pdfFileName, "pdfFileName");
        requireText(pdfContentType, "pdfContentType");
        pdfContent = Arrays.copyOf(Objects.requireNonNull(pdfContent, "pdfContent is required"), pdfContent.length);
        if (jsonContent.length == 0 || pdfContent.length == 0) {
            throw new IllegalArgumentException("report content must not be empty");
        }
    }

    @Override
    public byte[] jsonContent() {
        return Arrays.copyOf(jsonContent, jsonContent.length);
    }

    @Override
    public byte[] pdfContent() {
        return Arrays.copyOf(pdfContent, pdfContent.length);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
