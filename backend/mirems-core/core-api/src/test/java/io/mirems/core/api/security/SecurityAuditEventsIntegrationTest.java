package io.mirems.core.api.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import io.mirems.core.domain.audit.AuditEvent;
import io.mirems.core.domain.audit.AuditEventRepository;
import io.mirems.core.infra.audit.InMemoryAuditEventRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "server.servlet.context-path=",
            "mirems.security.rate-limit.enabled=false",
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8081/realms/mirems",
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
class SecurityAuditEventsIntegrationTest {
    private static final UUID ELECTION_A_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ELECTION_B_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @LocalServerPort
    int port;

    @Autowired
    AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    @Test
    void invalidBearerTokenIsAuditedAsSecurityViolation() {
        int before = auditEventRepository.findByEventType("SECURITY_VIOLATION").size();

        given()
                .auth().oauth2("invalid-token")
                .accept("application/problem+json")
        .when()
                .get("/audit")
        .then()
                .statusCode(401)
                .header("WWW-Authenticate", containsString("Bearer"));

        assertThat(newSecurityViolationsAfter(before))
                .anySatisfy(event -> {
                    assertThat(event.getActorId()).isEqualTo("anonymous");
                    assertThat(event.getAggregateType()).isEqualTo("Security");
                    assertThat(event.getPayload())
                            .containsEntry("violationType", "AUTHENTICATION_FAILURE")
                            .containsEntry("reason", "AUTHENTICATION_FAILED")
                            .containsEntry("path", "/audit")
                            .containsEntry("method", "GET");
                    assertThat(event.getPayload().toString()).doesNotContain("invalid-token");
                });
    }

    @Test
    void invalidBasicCredentialIsAuditedAsSecurityViolation() {
        int before = auditEventRepository.findByEventType("SECURITY_VIOLATION").size();

        given()
                .auth().preemptive().basic("legacy-observer", "wrong-password")
                .accept("application/problem+json")
                .header("X-Forwarded-For", "203.0.113.99")
        .when()
                .get("/audit")
        .then()
                .statusCode(401)
                .header("WWW-Authenticate", containsString("Basic"));

        assertThat(newSecurityViolationsAfter(before))
                .anySatisfy(event -> {
                    assertThat(event.getPayload())
                            .containsEntry("violationType", "AUTHENTICATION_FAILURE")
                            .containsEntry("reason", "AUTHENTICATION_FAILED")
                            .containsEntry("path", "/audit")
                            .containsEntry("method", "GET");
                    assertThat(event.getSourceIp()).isNotEqualTo("203.0.113.99");
                });
    }

    @Test
    void roleViolationIsAuditedAsSecurityViolation() {
        int before = auditEventRepository.findByEventType("SECURITY_VIOLATION").size();

        given()
                .auth().oauth2("observer-token")
                .accept("application/problem+json")
        .when()
                .get("/audit")
        .then()
                .statusCode(403);

        assertThat(newSecurityViolationsAfter(before))
                .anySatisfy(event -> assertThat(event.getPayload())
                        .containsEntry("violationType", "AUTHORIZATION_FAILURE")
                        .containsEntry("reason", "AUTHORIZATION_DENIED")
                        .containsEntry("actorId", "observer-1")
                        .containsEntry("path", "/audit")
                        .containsEntry("method", "GET"));
    }

    @Test
    void electionScopeViolationIsAuditedAsSecurityViolation() {
        int before = auditEventRepository.findByEventType("SECURITY_VIOLATION").size();

        given()
                .auth().oauth2("election-a-observer-token")
                .accept("application/problem+json")
        .when()
                .get("/elections/{id}", ELECTION_B_ID)
        .then()
                .statusCode(403);

        assertThat(newSecurityViolationsAfter(before))
                .anySatisfy(event -> assertThat(event.getPayload())
                        .containsEntry("violationType", "ELECTION_SCOPE_VIOLATION")
                        .containsEntry("actorId", "observer-a")
                        .containsEntry("electionId", ELECTION_B_ID.toString())
                        .containsEntry("path", "/elections/" + ELECTION_B_ID));
    }

    @Test
    void successfulJwtAuthenticationIsAudited() {
        int before = auditEventRepository.findByEventType("AUTHENTICATION_SUCCESS").size();

        given()
                .auth().oauth2("auditor-token")
                .accept(ContentType.JSON)
        .when()
                .get("/audit")
        .then()
                .statusCode(200);

        assertThat(auditEventRepository.findByEventType("AUTHENTICATION_SUCCESS").stream().skip(before))
                .anySatisfy(event -> assertThat(event.getPayload())
                        .containsEntry("actorId", "auditor-1")
                        .containsEntry("authenticationType", "JWT")
                        .containsEntry("path", "/audit")
                        .containsEntry("method", "GET"));
    }

    private List<AuditEvent> newSecurityViolationsAfter(int before) {
        return auditEventRepository.findByEventType("SECURITY_VIOLATION").stream().skip(before).toList();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityAuditTestConfig {
        @Bean
        AuditEventRepository auditEventRepository() {
            return new InMemoryAuditEventRepository();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> switch (token) {
                case "auditor-token" -> jwt("auditor-1", List.of("AUDITOR"), List.of("*"));
                case "observer-token" -> jwt("observer-1", List.of("OBSERVER"), List.of("*"));
                case "election-a-observer-token" -> jwt("observer-a", List.of("OBSERVER"), List.of(ELECTION_A_ID.toString()));
                default -> throw new BadJwtException("Unexpected test token: " + token);
            };
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername("legacy-observer")
                    .password("{noop}password")
                    .authorities("ROLE_OBSERVER", "ELECTION_SCOPE_" + ELECTION_A_ID)
                    .build());
        }

        private Jwt jwt(String subject, Collection<String> roles, Collection<String> electionScope) {
            Instant now = Instant.now();
            return new Jwt(
                    subject + "-token",
                    now,
                    now.plusSeconds(300),
                    Map.of("alg", "none"),
                    Map.of(
                            "sub", subject,
                            "preferred_username", subject,
                            "realm_access", Map.of("roles", roles),
                            "mirems_election_scope", electionScope));
        }
    }
}
