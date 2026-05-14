package io.mirems.core.api.audit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import io.mirems.core.api.MiremsCoreApiApplication;
import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import io.restassured.RestAssured;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = {MiremsCoreApiApplication.class, AuditLogControllerIntegrationTest.TestSecurityUsers.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
class AuditLogControllerIntegrationTest {
    private static final UUID AGGREGATE_ID = UUID.fromString("36000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_AGGREGATE_ID = UUID.fromString("36000000-0000-0000-0000-000000000002");

    @LocalServerPort
    int port;

    @MockitoBean
    AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        when(auditEventRepository.findAllChronologically()).thenReturn(List.of(
                event("36000000-0000-0000-0000-000000000011", AGGREGATE_ID, "Election", "ElectionCreated", "2026-06-03T09:00:00Z"),
                event("36000000-0000-0000-0000-000000000012", OTHER_AGGREGATE_ID, "VotingSession", "VoteCast", "2026-06-03T09:02:00Z"),
                event("36000000-0000-0000-0000-000000000013", AGGREGATE_ID, "Election", "ElectionClosed", "2026-06-03T09:04:00Z"),
                event("36000000-0000-0000-0000-000000000014", AGGREGATE_ID, "Election", "ElectionCertified", "2026-06-03T09:06:00Z")));
    }

    @Test
    void auditorCanFilterAuditEventsByAggregateAndTimeWindowWithPagination() {
        given()
                .auth().preemptive().basic("auditor", "password")
                .queryParam("aggregateId", AGGREGATE_ID)
                .queryParam("aggregateType", "Election")
                .queryParam("from", "2026-06-03T09:03:00Z")
                .queryParam("to", "2026-06-03T09:06:00Z")
                .queryParam("page", 0)
                .queryParam("size", 1)
        .when()
                .get("/audit")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("page", equalTo(0))
                .body("size", equalTo(1))
                .body("totalElements", equalTo(2))
                .body("totalPages", equalTo(2))
                .body("content", hasSize(1))
                .body("content[0].id", equalTo("36000000-0000-0000-0000-000000000013"))
                .body("content[0].eventType", equalTo("ElectionClosed"))
                .body("content[0].aggregateId", equalTo(AGGREGATE_ID.toString()))
                .body("content[0].aggregateType", equalTo("Election"))
                .body("content[0].actorId", equalTo("election-admin"))
                .body("content[0].occurredAt", equalTo("2026-06-03T09:04:00Z"))
                .body("content[0].sourceIp", equalTo("10.0.0.13"))
                .body("content[0].payload.action", equalTo("ElectionClosed"));
    }

    @Test
    void systemAdminCanReadSecondAuditPage() {
        given()
                .auth().preemptive().basic("system-admin", "password")
                .queryParam("aggregateId", AGGREGATE_ID)
                .queryParam("page", 1)
                .queryParam("size", 2)
        .when()
                .get("/audit")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("page", equalTo(1))
                .body("size", equalTo(2))
                .body("totalElements", equalTo(3))
                .body("totalPages", equalTo(2))
                .body("content", hasSize(1))
                .body("content[0].eventType", equalTo("ElectionCertified"));
    }

    @Test
    void auditEndpointRequiresAuditorOrSystemAdminRole() {
        given()
        .when()
                .get("/audit")
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given()
                .auth().preemptive().basic("voter", "password")
        .when()
                .get("/audit")
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void auditEndpointRejectsInvalidPaginationAndInvalidTimeWindow() {
        given()
                .auth().preemptive().basic("auditor", "password")
                .queryParam("page", -1)
        .when()
                .get("/audit")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Invalid audit query"));

        given()
                .auth().preemptive().basic("auditor", "password")
                .queryParam("from", "2026-06-03T10:00:00Z")
                .queryParam("to", "2026-06-03T09:00:00Z")
        .when()
                .get("/audit")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Invalid audit query"));
    }

    private static AuditEvent event(String id, UUID aggregateId, String aggregateType, String eventType, String occurredAt) {
        return AuditEvent.create(
                UUID.fromString(id),
                eventType,
                aggregateId,
                aggregateType,
                Map.of("action", eventType),
                "election-admin",
                OffsetDateTime.parse(occurredAt),
                "10.0.0." + id.substring(id.length() - 2));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityUsers {
        @Bean
        UserDetailsService testUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("auditor")
                            .password("{noop}password")
                            .roles("AUDITOR")
                            .build(),
                    User.withUsername("system-admin")
                            .password("{noop}password")
                            .roles("SYSTEM_ADMIN")
                            .build(),
                    User.withUsername("voter")
                            .password("{noop}password")
                            .roles("VOTER")
                            .build());
        }
    }
}
