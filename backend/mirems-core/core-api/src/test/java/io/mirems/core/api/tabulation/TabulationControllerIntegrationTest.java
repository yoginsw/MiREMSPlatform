package io.mirems.core.api.tabulation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.api.MiremsCoreApiApplication;
import io.mirems.core.bpmn.tabulation.BallotTabulationProcessService;
import io.mirems.core.bpmn.tabulation.BallotTabulationRequest;
import io.mirems.core.bpmn.tabulation.BallotTabulationResult;
import io.mirems.core.domain.election.Election;
import io.mirems.core.domain.election.ElectionRepository;
import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.result.ContestTally;
import io.mirems.core.domain.result.TabulationCompletedEvent;
import io.mirems.core.domain.result.TabulationReport;
import io.mirems.core.domain.result.TabulationReportRepository;
import io.restassured.RestAssured;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
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
        classes = {MiremsCoreApiApplication.class, TabulationControllerIntegrationTest.TestSecurityUsers.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
class TabulationControllerIntegrationTest {
    private static final UUID ELECTION_ID = UUID.fromString("35000000-0000-0000-0000-000000000001");
    private static final UUID REPORT_ID = UUID.fromString("35000000-0000-0000-0000-000000000002");
    private static final UUID CONTEST_ID = UUID.fromString("35000000-0000-0000-0000-000000000003");
    private static final UUID CANDIDATE_ID = UUID.fromString("35000000-0000-0000-0000-000000000004");
    private static final OffsetDateTime GENERATED_AT = OffsetDateTime.parse("2026-06-03T21:00:00Z");

    @LocalServerPort
    int port;

    @MockitoBean
    BallotTabulationProcessService tabulationProcessService;

    @MockitoBean
    ElectionRepository electionRepository;

    @MockitoBean
    TabulationReportRepository tabulationReportRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/miremsplatform";
    }

    @Test
    void tabulationOfficerCanTriggerTabulationWorkflow() {
        Election closedElection = closedElection();
        TabulationReport report = lockedReport(false);
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(closedElection));
        when(tabulationProcessService.tabulate(any(BallotTabulationRequest.class)))
                .thenReturn(new BallotTabulationResult(
                        true,
                        false,
                        report,
                        new TabulationCompletedEvent(REPORT_ID, ELECTION_ID, report.getHash(), report.getLockedAt())));

        given()
                .auth().preemptive().basic("tabulation-officer", "password")
        .when()
                .post("/elections/{electionId}/tabulate", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.ACCEPTED.value())
                .body("processId", equalTo("BallotTabulationProcess"))
                .body("status", equalTo("COMPLETED"))
                .body("variables.electionId", equalTo(ELECTION_ID.toString()))
                .body("variables.reportId", equalTo(REPORT_ID.toString()))
                .body("variables.reportHash", equalTo(report.getHash()));

        ArgumentCaptor<BallotTabulationRequest> captor = ArgumentCaptor.forClass(BallotTabulationRequest.class);
        verify(tabulationProcessService).tabulate(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().electionId()).isEqualTo(ELECTION_ID);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().electionStatus()).isEqualTo(closedElection.getElectionStatus());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().reviewerRole()).isEqualTo("TABULATION_OFFICER");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().publicResults()).isFalse();
    }

    @Test
    void certifiedElectionResultsArePublic() {
        Election certifiedElection = certifiedElection();
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(certifiedElection));
        when(tabulationReportRepository.findByElectionId(ELECTION_ID)).thenReturn(Optional.of(lockedReport(true)));

        given()
        .when()
                .get("/elections/{electionId}/results", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("electionId", equalTo(ELECTION_ID.toString()))
                .body("status", equalTo("CERTIFIED"))
                .body("generatedAt", equalTo("2026-06-03T21:00:00Z"))
                .body("contestTallies", hasSize(1))
                .body("contestTallies[0].contestId", equalTo(CONTEST_ID.toString()))
                .body("contestTallies[0].candidateTallies[0].candidateId", equalTo(CANDIDATE_ID.toString()))
                .body("contestTallies[0].candidateTallies[0].voteCount", equalTo(7));
    }

    @Test
    void nonCertifiedResultsAreRestrictedToElectionOfficials() {
        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(closedElection()));
        when(tabulationReportRepository.findByElectionId(ELECTION_ID)).thenReturn(Optional.of(lockedReport(false)));

        given()
        .when()
                .get("/elections/{electionId}/results", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Authentication required"));

        given()
                .auth().preemptive().basic("voter", "password")
        .when()
                .get("/elections/{electionId}/results", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .contentType("application/problem+json");

        given()
                .auth().preemptive().basic("election-officer", "password")
        .when()
                .get("/elections/{electionId}/results", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    void tabulationEndpointsReturnProblemDetailsForForbiddenMissingAndUnavailableCases() {
        given()
                .auth().preemptive().basic("voter", "password")
        .when()
                .post("/elections/{electionId}/tabulate", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.empty());
        given()
                .auth().preemptive().basic("tabulation-officer", "password")
        .when()
                .post("/elections/{electionId}/tabulate", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Election not found"));

        when(electionRepository.findById(ELECTION_ID)).thenReturn(Optional.of(closedElection()));
        when(tabulationReportRepository.findByElectionId(ELECTION_ID)).thenReturn(Optional.empty());
        given()
                .auth().preemptive().basic("election-officer", "password")
        .when()
                .get("/elections/{electionId}/results", ELECTION_ID)
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Tabulation report not found"));
    }

    private static TabulationReport lockedReport(boolean published) {
        TabulationReport report = TabulationReport.draft(
                REPORT_ID,
                ELECTION_ID,
                Map.of(CONTEST_ID, new ContestTally(CONTEST_ID, Map.of(CANDIDATE_ID, 7), 7)),
                GENERATED_AT);
        report.lock(GENERATED_AT.plusMinutes(1));
        if (published) {
            report.markPublished();
        }
        return report;
    }

    private static Election closedElection() {
        Election election = Election.create(ELECTION_ID, "General Election", ElectionType.LOCAL, "Seoul", LocalDate.of(2026, 6, 3), "KR", "ext-kr");
        election.publish();
        election.activate();
        election.close();
        return election;
    }

    private static Election certifiedElection() {
        Election election = closedElection();
        election.certify();
        return election;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityUsers {
        @Bean
        UserDetailsService testUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("tabulation-officer")
                            .password("{noop}password")
                            .authorities("ROLE_TABULATION_OFFICER", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build(),
                    User.withUsername("election-officer")
                            .password("{noop}password")
                            .authorities("ROLE_ELECTION_OFFICER", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build(),
                    User.withUsername("voter")
                            .password("{noop}password")
                            .authorities("ROLE_VOTER", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build());
        }
    }
}
