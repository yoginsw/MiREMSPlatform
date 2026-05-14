package io.mirems.core.infra.audit;

import io.mirems.core.domain.audit.AuditEventRepository;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/** Spring wiring for append-only audit publication when Kafka is configured. */
@Configuration
public class AuditEventPublisherConfiguration {
    @Bean
    @ConditionalOnMissingBean
    Clock auditClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(AuditEventRepository.class)
    AuditEventRepository auditEventRepository() {
        return new InMemoryAuditEventRepository();
    }

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean
    AuditEventPublisher auditEventPublisher(
            AuditEventRepository auditEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            Clock auditClock) {
        return new AuditEventPublisher(auditEventRepository, kafkaTemplate, auditClock);
    }

    @Bean
    @ConditionalOnBean(AuditEventPublisher.class)
    @ConditionalOnMissingBean
    AuditActionAspect auditActionAspect(AuditEventPublisher publisher, Clock auditClock) {
        return new AuditActionAspect(publisher, auditClock);
    }
}
