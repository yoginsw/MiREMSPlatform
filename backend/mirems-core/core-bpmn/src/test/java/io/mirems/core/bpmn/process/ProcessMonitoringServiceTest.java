package io.mirems.core.bpmn.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessMonitoringServiceTest {
    @Test
    void registryListsOnlyActivePendingAndSuspendedProcessesInStableOrder() {
        InMemoryProcessInstanceRegistry registry = new InMemoryProcessInstanceRegistry();
        registry.record(new ProcessStatus("pi-003", "VoteCorrectionProcess", "COMPLETED", Map.of(), List.of()));
        registry.record(new ProcessStatus("pi-002", "AuditReviewProcess", "PENDING", Map.of(), List.of()));
        registry.record(new ProcessStatus("pi-001", "AuditReviewProcess", "ACTIVE", Map.of(), List.of("review")));
        registry.record(new ProcessStatus("pi-004", "ResultCertificationProcess", "SUSPENDED", Map.of(), List.of()));

        assertThat(registry.listActiveProcesses())
                .extracting(ProcessStatus::instanceId)
                .containsExactly("pi-001", "pi-002", "pi-004");
    }

    @Test
    void monitoringServiceDelegatesSignalAndRecordsUpdatedStatus() {
        @SuppressWarnings("unchecked")
        MiremsProcessService<Map<String, Object>, ProcessStatus> processService = mock(MiremsProcessService.class);
        InMemoryProcessInstanceRegistry registry = new InMemoryProcessInstanceRegistry();
        DefaultProcessMonitoringService monitoringService = new DefaultProcessMonitoringService(processService, registry);
        ProcessSignalCommand command = new ProcessSignalCommand("approve", Map.of("approved", true));
        ProcessStatus updated = new ProcessStatus("pi-001", "ElectionPublicationProcess", "ACTIVE", Map.of("approved", true), List.of());
        when(processService.signalProcess("pi-001", "approve", Map.of("approved", true))).thenReturn(updated);

        assertThat(monitoringService.signalProcess("pi-001", command)).isEqualTo(updated);
        assertThat(monitoringService.listActiveProcesses()).containsExactly(updated);
        assertThat(monitoringService.getAuditTrail("pi-001")).isEmpty();
        verify(processService).signalProcess("pi-001", "approve", Map.of("approved", true));
    }

    @Test
    void signalCommandRejectsBlankSignalNameAndDefensivelyCopiesPayload() {
        assertThatThrownBy(() -> new ProcessSignalCommand(" ", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("signalName is required");

        ProcessSignalCommand command = new ProcessSignalCommand(" approve ", Map.of("approved", true));

        assertThat(command.signalName()).isEqualTo("approve");
        assertThatThrownBy(() -> command.payload().put("tamper", false))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void processAuditEntryRejectsMissingFieldsAndDefensivelyCopiesPayload() {
        assertThatThrownBy(() -> new ProcessAuditEntry(" ", "Process", "STARTED", "admin", OffsetDateTime.now(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("instanceId is required");
        assertThatThrownBy(() -> new ProcessAuditEntry("pi-001", "Process", "STARTED", "admin", null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("occurredAt is required");

        ProcessAuditEntry entry = new ProcessAuditEntry(
                "pi-001",
                "AuditReviewProcess",
                "PROCESS_STARTED",
                "auditor-1",
                OffsetDateTime.parse("2026-06-05T10:15:30Z"),
                Map.of("status", "ACTIVE"));

        assertThat(entry.payload()).containsEntry("status", "ACTIVE");
        assertThatThrownBy(() -> entry.payload().put("tamper", true))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void processMonitoringConfigurationCreatesServiceForKogitoAdapter() {
        @SuppressWarnings("unchecked")
        KogitoProcessAdapter adapter = mock(KogitoProcessAdapter.class);
        InMemoryProcessInstanceRegistry registry = new InMemoryProcessInstanceRegistry();

        ProcessMonitoringService service = new ProcessMonitoringConfiguration().processMonitoringService(adapter, registry);

        assertThat(service.listActiveProcesses()).isEmpty();
    }
}
