package io.mirems.core.bpmn.process;

import java.util.List;
import java.util.Map;

public record ProcessStatus(
        String instanceId,
        String processId,
        String status,
        Map<String, Object> variables,
        List<String> activeNodes) {
    public ProcessStatus {
        variables = Map.copyOf(variables == null ? Map.of() : variables);
        activeNodes = List.copyOf(activeNodes == null ? List.of() : activeNodes);
    }
}
