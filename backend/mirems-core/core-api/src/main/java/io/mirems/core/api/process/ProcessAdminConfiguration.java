package io.mirems.core.api.process;

import io.mirems.core.bpmn.process.ProcessAuditEntry;
import io.mirems.core.bpmn.process.ProcessMonitoringService;
import io.mirems.core.bpmn.process.ProcessSignalCommand;
import io.mirems.core.bpmn.process.ProcessStatus;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ProcessAdminConfiguration {
    @Bean
    @ConditionalOnMissingBean(ProcessMonitoringService.class)
    ProcessMonitoringService fallbackProcessMonitoringService() {
        return new ProcessMonitoringService() {
            @Override
            public List<ProcessStatus> listActiveProcesses() {
                return List.of();
            }

            @Override
            public ProcessStatus signalProcess(String instanceId, ProcessSignalCommand command) {
                return new ProcessStatus(instanceId, "unknown", "UNKNOWN", command.payload(), List.of());
            }

            @Override
            public List<ProcessAuditEntry> getAuditTrail(String instanceId) {
                return List.of();
            }
        };
    }
}
