package io.mirems.core.bpmn.process;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryProcessInstanceRegistry implements ProcessInstanceRegistry {
    private final ConcurrentMap<String, ProcessStatus> processes = new ConcurrentHashMap<>();

    @Override
    public void record(ProcessStatus processStatus) {
        processes.put(processStatus.instanceId(), processStatus);
    }

    @Override
    public List<ProcessStatus> listActiveProcesses() {
        return processes.values().stream()
                .filter(status -> "ACTIVE".equals(status.status()) || "PENDING".equals(status.status()) || "SUSPENDED".equals(status.status()))
                .sorted(Comparator.comparing(ProcessStatus::processId).thenComparing(ProcessStatus::instanceId))
                .toList();
    }
}
