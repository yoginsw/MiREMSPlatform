package io.mirems.core.api.security;

import io.mirems.core.domain.audit.AuditEventRepository;
import io.mirems.core.infra.audit.InMemoryAuditEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SecurityAuditRepositoryConfiguration {
    @Bean
    @ConditionalOnMissingBean(AuditEventRepository.class)
    AuditEventRepository securityAuditEventRepository() {
        return new InMemoryAuditEventRepository();
    }
}
