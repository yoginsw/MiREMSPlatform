package io.mirems.core.bpmn.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MiremsProcessServiceContractTest {

    @Test
    void exposesOnlyHybridArchitectureEntryPointMethods() throws Exception {
        Method start = MiremsProcessService.class.getMethod(
                "startProcess", String.class, Object.class, String.class);
        Method signal = MiremsProcessService.class.getMethod(
                "signalProcess", String.class, String.class, Object.class);
        Method status = MiremsProcessService.class.getMethod("getStatus", String.class);

        assertThat(start.getReturnType()).isEqualTo(Object.class);
        assertThat(signal.getReturnType()).isEqualTo(Object.class);
        assertThat(status.getReturnType()).isEqualTo(ProcessStatus.class);
        assertThat(MiremsProcessService.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactlyInAnyOrder("startProcess", "signalProcess", "getStatus");
    }

    @Test
    void processStatusIsImmutableSnapshotOfProcessEvidence() {
        Map<String, Object> variables = Map.of("outcome", "published");
        List<String> activeNodes = List.of("Review election configuration");

        ProcessStatus status = new ProcessStatus(
                "pi-001",
                "election-publication",
                "ACTIVE",
                variables,
                activeNodes);

        assertThat(status.instanceId()).isEqualTo("pi-001");
        assertThat(status.processId()).isEqualTo("election-publication");
        assertThat(status.status()).isEqualTo("ACTIVE");
        assertThat(status.variables()).containsEntry("outcome", "published");
        assertThat(status.activeNodes()).containsExactly("Review election configuration");
    }
}
