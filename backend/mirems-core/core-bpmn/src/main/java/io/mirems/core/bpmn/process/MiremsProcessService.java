package io.mirems.core.bpmn.process;

public interface MiremsProcessService<I, O> {
    O startProcess(String processId, I input, String correlationId);

    O signalProcess(String instanceId, String signalName, Object payload);

    ProcessStatus getStatus(String instanceId);
}
