package io.mirems.core.bpmn.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.kogito.Model;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceReadMode;
import org.kie.kogito.process.ProcessInstances;
import org.kie.kogito.process.Processes;

class KogitoProcessAdapterTest {
    private Processes processes;
    private org.kie.kogito.process.ProcessService kogitoProcessService;
    private KogitoProcessAdapter adapter;

    @BeforeEach
    void setUp() {
        processes = mock(Processes.class);
        kogitoProcessService = mock(org.kie.kogito.process.ProcessService.class);
        adapter = new KogitoProcessAdapter(processes, kogitoProcessService);
    }

    @Test
    void startsProcessThroughKogitoApiAndReturnsProcessStatus() {
        org.kie.kogito.process.Process<Model> process = mockProcess("election-publication");
        Model model = mock(Model.class);
        ProcessInstance<Model> instance = mockInstance("pi-001", ProcessInstance.STATE_COMPLETED, model);
        doReturn(process).when(processes).processById("election-publication");
        when(process.createModel()).thenReturn(model);
        when(kogitoProcessService.createProcessInstance(eq(process), eq("corr-001"), eq(model), isNull()))
                .thenReturn(instance);

        ProcessStatus status = adapter.startProcess(
                "election-publication",
                Map.of("electionId", "election-001"),
                "corr-001");

        verify(model).update(Map.of("electionId", "election-001"));
        verify(instance).start();
        assertThat(status.instanceId()).isEqualTo("pi-001");
        assertThat(status.processId()).isEqualTo("election-publication");
        assertThat(status.status()).isEqualTo("COMPLETED");
    }

    @Test
    void signalsProcessThroughKogitoApiThenReturnsCurrentStatus() {
        org.kie.kogito.process.Process<Model> process = mockProcess("election-publication");
        ProcessInstances<Model> instances = mock(ProcessInstances.class);
        Model variables = mock(Model.class);
        ProcessInstance<Model> instance = mockInstance("pi-002", ProcessInstance.STATE_ACTIVE, variables);
        when(processes.processByProcessInstanceId("pi-002")).thenReturn(Optional.of(process));
        when(process.instances()).thenReturn(instances);
        when(instances.findById("pi-002", ProcessInstanceReadMode.READ_ONLY)).thenReturn(Optional.of(instance));
        org.kie.kogito.process.Process rawProcess = process;
        doReturn(Optional.of(variables))
                .when(kogitoProcessService)
                .signalProcessInstance(rawProcess, "pi-002", Map.of("approved", true), "approve");

        ProcessStatus status = adapter.signalProcess("pi-002", "approve", Map.of("approved", true));

        assertThat(status.instanceId()).isEqualTo("pi-002");
        assertThat(status.processId()).isEqualTo("election-publication");
        assertThat(status.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getsStatusFromOwningKogitoProcess() {
        org.kie.kogito.process.Process<Model> process = mockProcess("audit-review");
        ProcessInstances<Model> instances = mock(ProcessInstances.class);
        Model variables = mock(Model.class);
        ProcessInstance<Model> instance = mockInstance("pi-003", ProcessInstance.STATE_ABORTED, variables);
        when(processes.processByProcessInstanceId("pi-003")).thenReturn(Optional.of(process));
        when(process.instances()).thenReturn(instances);
        when(instances.findById("pi-003", ProcessInstanceReadMode.READ_ONLY)).thenReturn(Optional.of(instance));

        ProcessStatus status = adapter.getStatus("pi-003");

        assertThat(status).isEqualTo(new ProcessStatus("pi-003", "audit-review", "ABORTED", Map.of(), List.of()));
    }

    @Test
    void throwsClearExceptionWhenProcessIdIsUnknown() {
        when(processes.processById("missing-process")).thenReturn(null);

        assertThatThrownBy(() -> adapter.startProcess("missing-process", Map.of(), "corr-missing"))
                .isInstanceOf(KogitoProcessException.class)
                .hasMessageContaining("Kogito process not found: missing-process");
    }

    @Test
    void throwsClearExceptionWhenSignalTargetIsUnknown() {
        org.kie.kogito.process.Process<Model> process = mockProcess("audit-review");
        when(processes.processByProcessInstanceId("pi-missing")).thenReturn(Optional.of(process));
        org.kie.kogito.process.Process rawProcess = process;
        doReturn(Optional.empty())
                .when(kogitoProcessService)
                .signalProcessInstance(rawProcess, "pi-missing", Map.of(), "approve");

        assertThatThrownBy(() -> adapter.signalProcess("pi-missing", "approve", Map.of()))
                .isInstanceOf(KogitoProcessException.class)
                .hasMessageContaining("Kogito process instance not found for signal: pi-missing");
    }

    @SuppressWarnings("unchecked")
    private static org.kie.kogito.process.Process<Model> mockProcess(String id) {
        org.kie.kogito.process.Process<Model> process = mock(org.kie.kogito.process.Process.class);
        when(process.id()).thenReturn(id);
        return process;
    }

    @SuppressWarnings("unchecked")
    private static ProcessInstance<Model> mockInstance(String id, int status, Model variables) {
        ProcessInstance<Model> instance = mock(ProcessInstance.class);
        when(instance.id()).thenReturn(id);
        when(instance.status()).thenReturn(status);
        when(instance.variables()).thenReturn(variables);
        return instance;
    }
}
