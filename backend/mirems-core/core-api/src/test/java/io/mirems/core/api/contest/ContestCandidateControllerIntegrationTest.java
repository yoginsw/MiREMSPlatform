package io.mirems.core.api.contest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.api.MiremsCoreApiApplication;
import io.mirems.core.bpmn.candidate.CandidateRegistrationOutcome;
import io.mirems.core.bpmn.candidate.CandidateRegistrationProcessService;
import io.mirems.core.bpmn.candidate.CandidateRegistrationRequest;
import io.mirems.core.bpmn.candidate.CandidateRegistrationResult;
import io.mirems.core.domain.contest.Candidate;
import io.mirems.core.domain.contest.Contest;
import io.mirems.core.domain.contest.ContestType;
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
        classes = {MiremsCoreApiApplication.class, ContestCandidateControllerIntegrationTest.TestSecurityUsers.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
class ContestCandidateControllerIntegrationTest {
    private static final UUID ELECTION_ID = UUID.fromString("10000000-0000-0000-0000-000000000031");
    private static final UUID CONTEST_ID = UUID.fromString("20000000-0000-0000-0000-000000000031");
    private static final UUID CANDIDATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000031");

    @LocalServerPort
    int port;

    @MockitoBean
    ElectionManagementService electionManagementService;

    @MockitoBean
    CandidateRegistrationProcessService candidateRegistrationProcessService;

    private Election election;
    private Contest contest;
    private Candidate candidate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/miremsplatform";
        election = Election.create(
                ELECTION_ID,
                "2028 General Election",
                ElectionType.PRESIDENTIAL,
                "US-FED",
                LocalDate.of(2028, 11, 7),
                "US",
                "ext-us");
        contest = Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "President", 1, 1);
        candidate = contest.addCandidate(CANDIDATE_ID, "Alex Kim", "Independent");
    }

    @Test
    void electionAdminCanCreateListGetAndUpdateContests() {
        when(electionManagementService.addContest(any())).thenReturn(contest);
        when(electionManagementService.listContests(ELECTION_ID)).thenReturn(List.of(contest));
        when(electionManagementService.getContest(ELECTION_ID, CONTEST_ID)).thenReturn(Optional.of(contest));
        Contest updated = Contest.create(CONTEST_ID, election, ContestType.RANKED_CHOICE, "Mayor", 3, 3);
        when(electionManagementService.updateContest(any())).thenReturn(updated);

        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "President",
                          "type": "CANDIDATE_CHOICE",
                          "seats": 1
                        }
                        """)
        .when()
                .post("/elections/{electionId}/contests", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .header("Location", "/elections/" + ELECTION_ID + "/contests/" + CONTEST_ID)
                .body("id", equalTo(CONTEST_ID.toString()))
                .body("electionId", equalTo(ELECTION_ID.toString()))
                .body("title", equalTo("President"))
                .body("type", equalTo("CANDIDATE_CHOICE"))
                .body("seats", equalTo(1));

        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/contests", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body(".", hasSize(1))
                .body("[0].id", equalTo(CONTEST_ID.toString()));

        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/contests/{contestId}", ELECTION_ID, CONTEST_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(CONTEST_ID.toString()));

        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "Mayor",
                          "type": "RANKED_CHOICE",
                          "seats": 3
                        }
                        """)
        .when()
                .put("/elections/{electionId}/contests/{contestId}", ELECTION_ID, CONTEST_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("title", equalTo("Mayor"))
                .body("type", equalTo("RANKED_CHOICE"))
                .body("seats", equalTo(3));
    }

    @Test
    void electionOfficerCanRegisterCandidateAndTriggersCandidateRegistrationProcess() {
        when(electionManagementService.addCandidate(any())).thenReturn(candidate);
        when(candidateRegistrationProcessService.register(any()))
                .thenReturn(new CandidateRegistrationResult(
                        CandidateRegistrationOutcome.PENDING_REVIEW, null, false, List.of()));

        given()
                .auth().preemptive().basic("election-officer", "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "displayName": "Alex Kim",
                          "party": "Independent",
                          "externalReference": "candidate-ext-1"
                        }
                        """)
        .when()
                .post("/elections/{electionId}/contests/{contestId}/candidates", ELECTION_ID, CONTEST_ID)
        .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .body("id", equalTo(CANDIDATE_ID.toString()))
                .body("contestId", equalTo(CONTEST_ID.toString()))
                .body("displayName", equalTo("Alex Kim"))
                .body("party", equalTo("Independent"))
                .body("status", equalTo("PENDING"));

        ArgumentCaptor<CandidateRegistrationRequest> requestCaptor = ArgumentCaptor.forClass(CandidateRegistrationRequest.class);
        verify(candidateRegistrationProcessService).register(requestCaptor.capture());
        CandidateRegistrationRequest processRequest = requestCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(processRequest.candidate()).isSameAs(candidate);
        org.assertj.core.api.Assertions.assertThat(processRequest.reviewerRole()).isEqualTo("ELECTION_OFFICER");
        org.assertj.core.api.Assertions.assertThat(processRequest.residencyVerified()).isTrue();
        org.assertj.core.api.Assertions.assertThat(processRequest.candidateAge()).isEqualTo(35);
    }

    @Test
    void authenticatedUsersCanListAndGetCandidatesAndOfficerCanWithdraw() {
        when(electionManagementService.listCandidates(ELECTION_ID, CONTEST_ID)).thenReturn(List.of(candidate));
        when(electionManagementService.getCandidate(ELECTION_ID, CONTEST_ID, CANDIDATE_ID)).thenReturn(Optional.of(candidate));
        candidate.withdraw();
        when(electionManagementService.withdrawCandidate(CANDIDATE_ID, "election-officer", "127.0.0.1"))
                .thenReturn(candidate);

        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/contests/{contestId}/candidates", ELECTION_ID, CONTEST_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body(".", hasSize(1))
                .body("[0].id", equalTo(CANDIDATE_ID.toString()));

        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/contests/{contestId}/candidates/{candidateId}", ELECTION_ID, CONTEST_ID, CANDIDATE_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(CANDIDATE_ID.toString()))
                .body("status", equalTo("WITHDRAWN"));

        given()
                .auth().preemptive().basic("election-officer", "password")
                .header("X-Forwarded-For", "127.0.0.1")
        .when()
                .put("/elections/{electionId}/contests/{contestId}/candidates/{candidateId}/withdraw", ELECTION_ID, CONTEST_ID, CANDIDATE_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("WITHDRAWN"));
    }

    @Test
    void contestAndCandidateEndpointsRejectUnauthorizedAndForbiddenRequests() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"title\":\"President\",\"type\":\"CANDIDATE_CHOICE\",\"seats\":1}")
        .when()
                .post("/elections/{electionId}/contests", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given()
                .auth().preemptive().basic("observer", "password")
                .contentType(ContentType.JSON)
                .body("{\"title\":\"President\",\"type\":\"CANDIDATE_CHOICE\",\"seats\":1}")
        .when()
                .post("/elections/{electionId}/contests", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .auth().preemptive().basic("observer", "password")
                .contentType(ContentType.JSON)
                .body("{\"displayName\":\"Alex Kim\",\"party\":\"Independent\"}")
        .when()
                .post("/elections/{electionId}/contests/{contestId}/candidates", ELECTION_ID, CONTEST_ID)
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void validationAndMissingEntitiesReturnProblemDetail() {
        given()
                .auth().preemptive().basic("election-admin", "password")
                .contentType(ContentType.JSON)
                .body("{\"title\":\"\",\"type\":\"CANDIDATE_CHOICE\",\"seats\":0}")
        .when()
                .post("/elections/{electionId}/contests", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .contentType("application/problem+json");

        when(electionManagementService.getContest(ELECTION_ID, CONTEST_ID)).thenReturn(Optional.empty());
        given()
                .auth().preemptive().basic("observer", "password")
        .when()
                .get("/elections/{electionId}/contests/{contestId}", ELECTION_ID, CONTEST_ID)
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Contest not found"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityUsers {
        @Bean
        UserDetailsService testUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("election-admin")
                            .password("{noop}password")
                            .roles("ELECTION_ADMIN")
                            .build(),
                    User.withUsername("election-officer")
                            .password("{noop}password")
                            .roles("ELECTION_OFFICER")
                            .build(),
                    User.withUsername("observer")
                            .password("{noop}password")
                            .roles("OBSERVER")
                            .build());
        }
    }
}
