package io.mirems.core.bpmn.process;

import java.util.List;

public interface ProcessMonitoringService {
    List<ProcessStatus> listActiveProcesses();

    ProcessStatus signalProcess(String instanceId, ProcessSignalCommand command);

    List<ProcessAuditEntry> getAuditTrail(String instanceId);
}
