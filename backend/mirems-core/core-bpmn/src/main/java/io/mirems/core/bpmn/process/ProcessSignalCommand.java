package io.mirems.core.bpmn.process;

import java.util.Map;

public record ProcessSignalCommand(String signalName, Map<String, Object> payload) {
    public ProcessSignalCommand {
        if (signalName == null || signalName.isBlank()) {
            throw new IllegalArgumentException("signalName is required");
        }
        signalName = signalName.strip();
        payload = Map.copyOf(payload == null ? Map.of() : payload);
    }
}
