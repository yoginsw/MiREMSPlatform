package io.mirems.core.api.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.api.MiremsCoreApiApplication;
import io.mirems.core.infra.service.voting.VotingSessionService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = {MiremsCoreApiApplication.class, ApiSecurityHardeningIntegrationTest.TestSecurityUsers.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
            "mirems.security.rate-limit.enabled=true",
            "mirems.security.rate-limit.capacity=2",
            "mirems.security.rate-limit.refill-tokens=2",
            "mirems.security.rate-limit.refill-period=PT1H",
            "mirems.security.cors.frontend-origin=http://localhost:5173"
        })
class ApiSecurityHardeningIntegrationTest {
    private static final UUID SESSION_ID = UUID.fromString("37000000-0000-0000-0000-000000000001");
    private static final UUID CONTEST_ID = UUID.fromString("37000000-0000-0000-0000-000000000002");
    private static final UUID CANDIDATE_ID = UUID.fromString("37000000-0000-0000-0000-000000000003");
    private static final String RESULT_HASH = "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd";

    @LocalServerPort
    int port;

    @MockitoBean
    VotingSessionService votingSessionService;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/miremsplatform";
        when(votingSessionService.castBallot(any()))
                .thenReturn(new VotingSessionService.CastBallotReceipt(SESSION_ID, List.of(RESULT_HASH)));
    }

    @Test
    void sensitiveEndpointReturnsTooManyRequestsAfterConfiguredPerUserAndIpThreshold() {
        castVoteFrom("10.37.0.1").then().statusCode(HttpStatus.CREATED.value());
        castVoteFrom("10.37.0.1").then().statusCode(HttpStatus.CREATED.value());

        castVoteFrom("10.37.0.1")
                .then()
                .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                .contentType("application/problem+json")
                .header("Retry-After", notNullValue())
                .body("title", equalTo("Too many requests"));

        verify(votingSessionService, times(2)).castBallot(any());
    }

    @Test
    void securityHeadersArePresentOnApplicationResponses() {
        given()
        .when()
                .get("/actuator/health")
        .then()
                .statusCode(HttpStatus.OK.value())
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Frame-Options", "DENY")
                .header("Strict-Transport-Security", notNullValue())
                .header("Content-Security-Policy", notNullValue());
    }

    @Test
    void corsAllowsConfiguredFrontendOriginOnly() {
        given()
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .when()
                .options("/audit")
        .then()
                .statusCode(HttpStatus.OK.value())
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");

        given()
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .when()
                .options("/audit")
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    private Response castVoteFrom(String ipAddress) {
        return given()
                .auth().preemptive().basic("voter-037", "password")
                .header("X-Forwarded-For", ipAddress)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "selections": [
                            {
                              "contestId": "37000000-0000-0000-0000-000000000002",
                              "selectionIds": ["37000000-0000-0000-0000-000000000003"]
                            }
                          ]
                        }
                        """)
        .when()
                .post("/sessions/{sessionId}/cast", SESSION_ID);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityUsers {
        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername("voter-037")
                    .password("{noop}password")
                    .roles("VOTER")
                    .build());
        }
    }
}
