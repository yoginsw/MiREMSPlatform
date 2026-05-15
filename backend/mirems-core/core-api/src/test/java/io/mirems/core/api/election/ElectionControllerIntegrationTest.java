package io.mirems.core.api.election;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.api.MiremsCoreApiApplication;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.infra.service.election.ElectionManagementService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = {MiremsCoreApiApplication.class, ElectionControllerIntegrationTest.TestSecurityUsers.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
class ElectionControllerIntegrationTest {
    private static final UUID ELECTION_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @LocalServerPort
    private int port;

    @MockitoBean
    private ElectionManagementService electionManagementService;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/miremsplatform";
    }

    @Test
    void createElectionRequiresElectionAdminAndReturnsCreatedElection() {
        Election draft = draftElection();
        when(electionManagementService.createElection(any())).thenReturn(draft);

        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "name": "2026 Local Election",
                          "electionType": "LOCAL",
                          "jurisdiction": "Seoul",
                          "scheduledDate": "2026-06-03",
                          "countryCode": "KR",
                          "extensionPackId": "ext-kr"
                        }
                        """)
        .when()
                .post("/elections")
        .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo(ELECTION_ID.toString()))
                .body("name", equalTo("2026 Local Election"))
                .body("electionType", equalTo("LOCAL"))
                .body("status", equalTo("DRAFT"))
                .body("scheduledDate", equalTo("2026-06-03"))
                .body("countryCode", equalTo("KR"))
                .body("extensionPackId", equalTo("ext-kr"));

        ArgumentCaptor<ElectionManagementService.CreateElectionCommand> command =
                ArgumentCaptor.forClass(ElectionManagementService.CreateElectionCommand.class);
        verify(electionManagementService).createElection(command.capture());
        org.assertj.core.api.Assertions.assertThat(command.getValue().actorId()).isEqualTo("election-admin");
        org.assertj.core.api.Assertions.assertThat(command.getValue().sourceIp()).isNotBlank();
    }

    @Test
    void listAndGetElectionsRequireAuthenticationOnly() {
        Election draft = draftElection();
        when(electionManagementService.listElections()).thenReturn(List.of(draft));
        when(electionManagementService.getElection(ELECTION_ID)).thenReturn(Optional.of(draft));

        given()
                .auth().preemptive().basic("observer", "password")
                .accept(ContentType.JSON)
        .when()
                .get("/elections")
        .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(ELECTION_ID.toString()))
                .body("[0].name", equalTo("2026 Local Election"));

        given()
                .auth().preemptive().basic("observer", "password")
                .accept(ContentType.JSON)
        .when()
                .get("/elections/{id}", ELECTION_ID)
        .then()
                .statusCode(200)
                .body("id", equalTo(ELECTION_ID.toString()));
    }

    @Test
    void publishAndCloseRequireElectionAdmin() {
        Election published = draftElection();
        published.publish();
        Election closed = draftElection();
        closed.publish();
        closed.activate();
        closed.close();
        when(electionManagementService.publishElection(ELECTION_ID, "election-admin", "127.0.0.1"))
                .thenReturn(published);
        when(electionManagementService.closeElection(ELECTION_ID, "election-admin", "127.0.0.1"))
                .thenReturn(closed);

        given()
                .auth().preemptive().basic("election-admin", "password")
                .header("X-Forwarded-For", "127.0.0.1")
                .accept(ContentType.JSON)
        .when()
                .put("/elections/{id}/publish", ELECTION_ID)
        .then()
                .statusCode(200)
                .body("status", equalTo("PUBLISHED"));

        given()
                .auth().preemptive().basic("election-admin", "password")
                .header("X-Forwarded-For", "127.0.0.1")
                .accept(ContentType.JSON)
        .when()
                .put("/elections/{id}/close", ELECTION_ID)
        .then()
                .statusCode(200)
                .body("status", equalTo("CLOSED"));
    }

    @Test
    void unauthorizedAndForbiddenRequestsAreRejected() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
        .when()
                .post("/elections")
        .then()
                .statusCode(401);

        given()
                .auth().preemptive().basic("observer", "password")
                .contentType(ContentType.JSON)
                .body("{}")
        .when()
                .post("/elections")
        .then()
                .statusCode(403);
    }

    @Test
    void invalidCreateRequestReturnsProblemDetail() {
        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .accept("application/problem+json")
                .body("""
                        {
                          "name": "",
                          "electionType": "LOCAL",
                          "jurisdiction": "Seoul",
                          "scheduledDate": "2026-06-03",
                          "countryCode": "K",
                          "extensionPackId": "ext-kr"
                        }
                        """)
        .when()
                .post("/elections")
        .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("title", equalTo("Bad Request"));
    }

    @Test
    void missingElectionReturnsProblemDetail404() {
        when(electionManagementService.getElection(ELECTION_ID)).thenReturn(Optional.empty());

        given()
                .auth().preemptive().basic("observer", "password")
                .accept("application/problem+json")
        .when()
                .get("/elections/{id}", ELECTION_ID)
        .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("title", equalTo("Election not found"));
    }

    private static Election draftElection() {
        return Election.create(ELECTION_ID, "2026 Local Election", ElectionType.LOCAL, "Seoul", LocalDate.of(2026, 6, 3), "KR", "ext-kr");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityUsers {
        @Bean
        UserDetailsService testUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("election-admin")
                            .password("{noop}password")
                            .authorities("ROLE_ELECTION_ADMIN", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build(),
                    User.withUsername("observer")
                            .password("{noop}password")
                            .authorities("ROLE_OBSERVER", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build());
        }
    }
}
