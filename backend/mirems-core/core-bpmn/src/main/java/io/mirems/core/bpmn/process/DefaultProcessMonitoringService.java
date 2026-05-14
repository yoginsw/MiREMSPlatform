package io.mirems.core.bpmn.process;

import java.util.List;
import java.util.Map;
import java.util.Objects;
public class DefaultProcessMonitoringService implements ProcessMonitoringService {
    private final MiremsProcessService<Map<String, Object>, ProcessStatus> processService;
    private final ProcessInstanceRegistry registry;

    public DefaultProcessMonitoringService(
            MiremsProcessService<Map<String, Object>, ProcessStatus> processService,
            ProcessInstanceRegistry registry) {
        this.processService = Objects.requireNonNull(processService, "processService is required");
        this.registry = Objects.requireNonNull(registry, "registry is required");
    }

    @Override
    public List<ProcessStatus> listActiveProcesses() {
        return registry.listActiveProcesses();
    }

    @Override
    public ProcessStatus signalProcess(String instanceId, ProcessSignalCommand command) {
        ProcessStatus status = processService.signalProcess(instanceId, command.signalName(), command.payload());
        registry.record(status);
        return status;
    }

    @Override
    public List<ProcessAuditEntry> getAuditTrail(String instanceId) {
        return List.of();
    }
}
