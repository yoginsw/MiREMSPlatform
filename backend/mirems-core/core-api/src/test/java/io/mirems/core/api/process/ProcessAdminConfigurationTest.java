package io.mirems.core.api.process;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.bpmn.process.ProcessSignalCommand;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessAdminConfigurationTest {
    @Test
    void fallbackProcessMonitoringServiceReturnsSafeEmptyAdminData() {
        var service = new ProcessAdminConfiguration().fallbackProcessMonitoringService();

        assertThat(service.listActiveProcesses()).isEmpty();
        assertThat(service.getAuditTrail("pi-missing")).isEmpty();
        assertThat(service.signalProcess("pi-missing", new ProcessSignalCommand("noop", Map.of("source", "fallback"))))
                .satisfies(status -> {
                    assertThat(status.instanceId()).isEqualTo("pi-missing");
                    assertThat(status.processId()).isEqualTo("unknown");
                    assertThat(status.status()).isEqualTo("UNKNOWN");
                    assertThat(status.variables()).containsEntry("source", "fallback");
                });
    }
}
