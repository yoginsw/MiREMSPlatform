package io.mirems.core.api.ballot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.api.MiremsCoreApiApplication;
import io.mirems.core.domain.ballot.AccessibilityFeature;
import io.mirems.core.domain.ballot.Ballot;
import io.mirems.core.domain.ballot.BallotStyle;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.infra.service.ballot.BallotManagementService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
        classes = {MiremsCoreApiApplication.class, BallotControllerIntegrationTest.TestSecurityUsers.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
class BallotControllerIntegrationTest {
    private static final UUID ELECTION_ID = UUID.fromString("10000000-0000-0000-0000-000000000032");
    private static final UUID BALLOT_ID = UUID.fromString("20000000-0000-0000-0000-000000000032");
    private static final UUID STYLE_ID = UUID.fromString("30000000-0000-0000-0000-000000000032");
    private static final UUID CONTEST_ID = UUID.fromString("40000000-0000-0000-0000-000000000032");

    @LocalServerPort
    int port;

    @MockitoBean
    BallotManagementService ballotManagementService;

    private Election election;
    private Contest contest;
    private Ballot ballot;
    private BallotStyle ballotStyle;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/miremsplatform";
        election = Election.create(ELECTION_ID, "2028 General Election", ElectionType.PRESIDENTIAL,
                "US-FED", LocalDate.of(2028, 11, 7), "US", "ext-us");
        contest = Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "President", 1, 1);
        ballot = Ballot.create(BALLOT_ID, election);
        ballot.addContest(contest, 1, "President");
        ballotStyle = ballot.addStyle(STYLE_ID, "US-FED-EN", "US-FED", "en",
                Set.of(AccessibilityFeature.AUDIO, AccessibilityFeature.HIGH_CONTRAST));
    }

    @Test
    void electionAdminCanCreateListAndVersionBallots() {
        when(ballotManagementService.createBallot(any())).thenReturn(ballot);
        when(ballotManagementService.listBallots(ELECTION_ID)).thenReturn(List.of(ballot));
        Ballot versioned = Ballot.create(BALLOT_ID, election);
        versioned.addContest(contest, 1, "President");
        versioned.addContest(Contest.create(UUID.fromString("50000000-0000-0000-0000-000000000032"), election,
                ContestType.BALLOT_MEASURE, "Measure A", 1, 1), 2, "Measure A");
        when(ballotManagementService.createBallotVersion(any())).thenReturn(versioned);

        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .body("""
                        { "contestIds": ["40000000-0000-0000-0000-000000000032"] }
                        """)
        .when()
                .post("/elections/{electionId}/ballots", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .header("Location", "/elections/" + ELECTION_ID + "/ballots/" + BALLOT_ID)
                .body("id", equalTo(BALLOT_ID.toString()))
                .body("electionId", equalTo(ELECTION_ID.toString()))
                .body("ballotVersion", equalTo(3))
                .body("active", equalTo(false))
                .body("contests", hasSize(1))
                .body("contests[0].contestId", equalTo(CONTEST_ID.toString()))
                .body("styles", hasSize(1));

        ArgumentCaptor<BallotManagementService.CreateBallotCommand> createCaptor =
                ArgumentCaptor.forClass(BallotManagementService.CreateBallotCommand.class);
        verify(ballotManagementService).createBallot(createCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(createCaptor.getValue().actorId()).isEqualTo("election-admin");

        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/ballots", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body(".", hasSize(1))
                .body("[0].id", equalTo(BALLOT_ID.toString()));

        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "changeReason": "Add measure contest",
                          "contestIds": [
                            "40000000-0000-0000-0000-000000000032",
                            "50000000-0000-0000-0000-000000000032"
                          ]
                        }
                        """)
        .when()
                .post("/elections/{electionId}/ballots/{ballotId}/versions", ELECTION_ID, BALLOT_ID)
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("ballotVersion", equalTo(3))
                .body("contests", hasSize(2));
    }

    @Test
    void ballotPreviewReturnsLayoutJson() {
        when(ballotManagementService.previewBallot(ELECTION_ID, BALLOT_ID)).thenReturn(Optional.of(ballot));

        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/ballots/{ballotId}/preview", ELECTION_ID, BALLOT_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("ballotId", equalTo(BALLOT_ID.toString()))
                .body("layout.version", equalTo(3))
                .body("layout.contests", hasSize(1))
                .body("layout.contests[0].title", equalTo("President"));
    }

    @Test
    void electionAdminCanCrudBallotStyles() {
        when(ballotManagementService.createBallotStyle(any())).thenReturn(ballotStyle);
        when(ballotManagementService.listBallotStyles(ELECTION_ID)).thenReturn(List.of(ballotStyle));
        BallotStyle updated = ballot.addStyle(UUID.fromString("31000000-0000-0000-0000-000000000032"),
                "US-FED-ES", "US-FED", "es", Set.of(AccessibilityFeature.LARGE_TEXT));
        when(ballotManagementService.updateBallotStyle(any())).thenReturn(updated);

        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "ballotId": "20000000-0000-0000-0000-000000000032",
                          "styleCode": "US-FED-EN",
                          "district": "US-FED",
                          "language": "en",
                          "accessibilityFeatures": ["AUDIO", "HIGH_CONTRAST"]
                        }
                        """)
        .when()
                .post("/elections/{electionId}/ballot-styles", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .header("Location", "/elections/" + ELECTION_ID + "/ballot-styles/" + STYLE_ID)
                .body("id", equalTo(STYLE_ID.toString()))
                .body("ballotId", equalTo(BALLOT_ID.toString()))
                .body("styleCode", equalTo("US-FED-EN"))
                .body("language", equalTo("en"))
                .body("accessibilityFeatures", hasSize(2));

        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/ballot-styles", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body(".", hasSize(1))
                .body("[0].styleCode", equalTo("US-FED-EN"));

        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "ballotId": "20000000-0000-0000-0000-000000000032",
                          "styleCode": "US-FED-ES",
                          "district": "US-FED",
                          "language": "es",
                          "accessibilityFeatures": ["LARGE_TEXT"]
                        }
                        """)
        .when()
                .put("/elections/{electionId}/ballot-styles/{ballotStyleId}", ELECTION_ID, STYLE_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("styleCode", equalTo("US-FED-ES"))
                .body("language", equalTo("es"));

        given()
                .auth().preemptive().basic("election-admin", "password")
        .when()
                .delete("/elections/{electionId}/ballot-styles/{ballotStyleId}", ELECTION_ID, STYLE_ID)
        .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        verify(ballotManagementService).deleteBallotStyle(ELECTION_ID, STYLE_ID, "election-admin", "127.0.0.1");
    }

    @Test
    void ballotEndpointsRejectUnauthorizedForbiddenInvalidAndMissingRequests() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"contestIds\":[\"40000000-0000-0000-0000-000000000032\"]}")
        .when()
                .post("/elections/{electionId}/ballots", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given()
                .auth().preemptive().basic("observer", "password")
                .contentType(ContentType.JSON)
                .body("{\"contestIds\":[\"40000000-0000-0000-0000-000000000032\"]}")
        .when()
                .post("/elections/{electionId}/ballots", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .body("{\"contestIds\":[]}")
        .when()
                .post("/elections/{electionId}/ballots", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .contentType("application/problem+json");

        when(ballotManagementService.previewBallot(ELECTION_ID, BALLOT_ID)).thenReturn(Optional.empty());
        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/ballots/{ballotId}/preview", ELECTION_ID, BALLOT_ID)
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Ballot not found"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityUsers {
        @Bean
        UserDetailsService testUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("election-admin").password("{noop}password").authorities("ROLE_ELECTION_ADMIN", "ELECTION_SCOPE_" + ELECTION_ID).build(),
                    User.withUsername("observer").password("{noop}password").authorities("ROLE_OBSERVER", "ELECTION_SCOPE_" + ELECTION_ID).build());
        }
    }
}
