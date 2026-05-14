package io.mirems.core.infra.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringJUnitConfig(classes = AuditEventPublisherTest.AuditTestConfig.class)
@EmbeddedKafka(partitions = 1, topics = AuditEventPublisher.AUDIT_TOPIC)
class AuditEventPublisherTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-03T10:15:30Z");

    @Autowired
    private AuditEventRepository repository;

    @Autowired
    private AuditedTestService auditedTestService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void auditEventRepositoryExposesOnlySaveAndFindMethodsAndRejectsDuplicateIds() {
        List<String> methodNames = List.of(AuditEventRepository.class.getDeclaredMethods()).stream()
                .map(java.lang.reflect.Method::getName)
                .toList();

        assertThat(methodNames).contains("save", "findById", "findByAggregateId", "findByEventType", "findAllChronologically");
        assertThat(methodNames).allMatch(name -> name.equals("save") || name.startsWith("findBy") || name.equals("findAllChronologically"));
        assertThat(methodNames).noneMatch(name -> name.startsWith("delete") || name.startsWith("update"));

        AuditEvent event = AuditEvent.create(
                UUID.fromString("018f4b7f-1010-7111-8111-111111111111"),
                "ElectionPublished",
                UUID.fromString("018f4b7f-2020-7222-8222-222222222222"),
                "Election",
                Map.of("status", "PUBLISHED"),
                "admin-001",
                FIXED_INSTANT.atOffset(ZoneOffset.UTC),
                "127.0.0.1");

        repository.save(event);

        assertThatThrownBy(() -> repository.save(event))
                .isInstanceOf(DuplicateAuditEventException.class)
                .hasMessageContaining(event.getId().toString());
    }

    @Test
    void auditActionPersistsAuditEventAndPublishesToEmbeddedKafkaAfterSuccessfulReturn() {
        assertThat(AopUtils.isAopProxy(auditedTestService)).isTrue();
        try (Consumer<String, String> consumer = auditConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, AuditEventPublisher.AUDIT_TOPIC);

            String result = auditedTestService.publishElection("E-2026-LOCAL");

            assertThat(result).isEqualTo("published:E-2026-LOCAL");
            List<AuditEvent> events = repository.findByEventType("ElectionPublished");
            assertThat(events).hasSize(1);
            AuditEvent event = events.getFirst();
            assertThat(event.getAggregateType()).isEqualTo("Election");
            assertThat(event.getActorId()).isEqualTo("system-test");
            assertThat(event.getSourceIp()).isEqualTo("127.0.0.1");
            assertThat(event.getPayload()).containsEntry("method", "publishElection");
            assertThat(event.getPayload()).containsEntry("result", "published:E-2026-LOCAL");

            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, AuditEventPublisher.AUDIT_TOPIC);
            assertThat(record.key()).isEqualTo(event.getId().toString());
            assertThat(record.value()).contains("ElectionPublished");
            assertThat(record.value()).contains(event.getAggregateId().toString());
        }
    }

    @Test
    void auditActionDoesNotEmitAuditEventWhenServiceThrows() {
        assertThatThrownBy(() -> auditedTestService.failElectionPublish("E-2026-LOCAL"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("publish failed");

        assertThat(repository.findByEventType("ElectionPublishFailed")).isEmpty();
    }

    private Consumer<String, String> auditConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("audit-test-consumer", "true", embeddedKafka);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer()).createConsumer();
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AuditTestConfig {
        @Bean
        Clock auditClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }

        @Bean
        AuditEventRepository auditEventRepository() {
            return new InMemoryAuditEventRepository();
        }

        @Bean
        KafkaTemplate<String, String> kafkaTemplate(EmbeddedKafkaBroker embeddedKafka) {
            Map<String, Object> props = Map.of(
                    org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    embeddedKafka.getBrokersAsString(),
                    org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class,
                    org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class,
                    org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG,
                    "all");
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
        }

        @Bean
        AuditEventPublisher auditEventPublisher(AuditEventRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
            return new AuditEventPublisher(repository, kafkaTemplate, auditClock());
        }

        @Bean
        AuditActionAspect auditActionAspect(AuditEventPublisher publisher) {
            return new AuditActionAspect(publisher, auditClock());
        }

        @Bean
        AuditedTestService auditedTestService() {
            return new AuditedTestService();
        }
    }

    @Service
    static class AuditedTestService {
        @AuditAction(
                eventType = "ElectionPublished",
                aggregateType = "Election",
                actorId = "system-test",
                sourceIp = "127.0.0.1")
        public String publishElection(String electionCode) {
            return "published:" + electionCode;
        }

        @AuditAction(
                eventType = "ElectionPublishFailed",
                aggregateType = "Election",
                actorId = "system-test",
                sourceIp = "127.0.0.1")
        public String failElectionPublish(String electionCode) {
            throw new IllegalStateException("publish failed: " + electionCode);
        }
    }
}
