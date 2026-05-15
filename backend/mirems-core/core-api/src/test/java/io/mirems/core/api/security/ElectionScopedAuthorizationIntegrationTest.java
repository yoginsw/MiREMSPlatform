package io.mirems.core.api.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.infra.service.election.ElectionManagementService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
class ElectionScopedAuthorizationIntegrationTest {
    private static final UUID ELECTION_A_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID ELECTION_B_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @LocalServerPort
    int port;

    @MockitoBean
    ElectionManagementService electionManagementService;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
        when(electionManagementService.getElection(ELECTION_A_ID)).thenReturn(Optional.of(draftElection(ELECTION_A_ID)));
        when(electionManagementService.getElection(ELECTION_B_ID)).thenReturn(Optional.of(draftElection(ELECTION_B_ID)));
    }

    @Test
    void scopedJwtCanAccessElectionWithinAssignedScope() {
        given()
                .auth().oauth2("election-a-observer-token")
                .accept(ContentType.JSON)
        .when()
                .get("/elections/{id}", ELECTION_A_ID)
        .then()
                .statusCode(200)
                .body("id", equalTo(ELECTION_A_ID.toString()));
    }

    @Test
    void scopedJwtCannotAccessElectionOutsideAssignedScope() {
        given()
                .auth().oauth2("election-a-observer-token")
                .accept("application/problem+json")
        .when()
                .get("/elections/{id}", ELECTION_B_ID)
        .then()
                .statusCode(403);

        verify(electionManagementService, never()).getElection(ELECTION_B_ID);
    }

    @Test
    void unscopedJwtCannotAccessElectionScopedEndpoints() {
        given()
                .auth().oauth2("unscoped-observer-token")
                .accept("application/problem+json")
        .when()
                .get("/elections/{id}", ELECTION_A_ID)
        .then()
                .statusCode(403);
    }

    @Test
    void nonJwtAuthenticationWithoutElectionScopeCannotAccessElectionScopedEndpoints() {
        given()
                .auth().preemptive().basic("legacy-observer", "password")
                .accept("application/problem+json")
        .when()
                .get("/elections/{id}", ELECTION_A_ID)
        .then()
                .statusCode(403);
    }

    private static Election draftElection(UUID electionId) {
        return Election.create(
                electionId,
                "2026 Local Election",
                ElectionType.LOCAL,
                "Seoul",
                LocalDate.of(2026, 6, 3),
                "KR",
                "ext-kr");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtDecoderConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> switch (token) {
                case "election-a-observer-token" -> jwt("observer-a", List.of("OBSERVER"), List.of(ELECTION_A_ID.toString()));
                case "unscoped-observer-token" -> jwt("observer-unscoped", List.of("OBSERVER"), List.of());
                default -> throw new IllegalArgumentException("Unexpected test token: " + token);
            };
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername("legacy-observer")
                    .password("{noop}password")
                    .authorities("ROLE_OBSERVER")
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
