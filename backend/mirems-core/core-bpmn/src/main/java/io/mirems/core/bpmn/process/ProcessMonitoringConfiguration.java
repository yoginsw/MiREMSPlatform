package io.mirems.core.bpmn.process;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ProcessMonitoringConfiguration {
    @Bean
    @ConditionalOnBean(KogitoProcessAdapter.class)
    ProcessMonitoringService processMonitoringService(
            KogitoProcessAdapter processService,
            ProcessInstanceRegistry registry) {
        MiremsProcessService<Map<String, Object>, ProcessStatus> miremsProcessService = processService;
        return new DefaultProcessMonitoringService(miremsProcessService, registry);
    }
}
