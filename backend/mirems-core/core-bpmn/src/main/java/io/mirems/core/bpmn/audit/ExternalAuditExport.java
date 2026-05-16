package io.mirems.core.bpmn.audit;

import java.util.Arrays;

public record ExternalAuditExport(String fileName, String contentType, byte[] content) {
    public ExternalAuditExport {
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
