package io.mirems.core.api.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import io.mirems.core.domain.audit.AuditEventRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
class KeycloakJwtSecurityIntegrationTest {
    @LocalServerPort
    int port;

    @MockitoBean
    AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        when(auditEventRepository.findAllChronologically()).thenReturn(List.of());
    }

    @Test
    void auditEndpointAcceptsKeycloakRealmRoleFromJwt() {
        given()
                .auth().oauth2("auditor-token")
                .accept(ContentType.JSON)
        .when()
                .get("/audit")
        .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    void auditEndpointRejectsAuthenticatedJwtWithoutRequiredRole() {
        given()
                .auth().oauth2("observer-token")
                .accept("application/problem+json")
        .when()
                .get("/audit")
        .then()
                .statusCode(403);
    }

    @Test
    void protectedEndpointRequiresBearerToken() {
        given()
                .accept("application/problem+json")
        .when()
                .get("/audit")
        .then()
                .statusCode(401);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtDecoderConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> switch (token) {
                case "auditor-token" -> jwt("auditor-1", List.of("AUDITOR"), List.of("election-1"));
                case "observer-token" -> jwt("observer-1", List.of("OBSERVER"), List.of("election-1"));
                default -> throw new IllegalArgumentException("Unexpected test token: " + token);
            };
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
