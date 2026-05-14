package io.mirems.core.bpmn.process;

import java.util.List;

public interface ProcessInstanceRegistry {
    void record(ProcessStatus processStatus);

    List<ProcessStatus> listActiveProcesses();
}
