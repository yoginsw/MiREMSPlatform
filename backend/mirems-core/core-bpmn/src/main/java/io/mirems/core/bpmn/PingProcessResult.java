package io.mirems.core.bpmn;

public record PingProcessResult(
        String instanceId,
        String processId,
        String status,
        String output) {
}
