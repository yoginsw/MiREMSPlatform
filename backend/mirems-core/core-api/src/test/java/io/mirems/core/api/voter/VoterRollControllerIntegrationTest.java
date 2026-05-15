package io.mirems.core.api.voter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mirems.core.api.MiremsCoreApiApplication;
import io.mirems.core.bpmn.voter.VoterEligibilityResult;
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.core.infra.service.voting.VoterRollService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
        classes = {MiremsCoreApiApplication.class, VoterRollControllerIntegrationTest.TestSecurityUsers.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
class VoterRollControllerIntegrationTest {
    private static final UUID VOTER_ID = UUID.fromString("10000000-0000-0000-0000-000000000033");
    private static final UUID OTHER_VOTER_ID = UUID.fromString("10000000-0000-0000-0000-000000000034");
    private static final UUID ELECTION_ID = UUID.fromString("20000000-0000-0000-0000-000000000033");
    private static final String EXTERNAL_VOTER_ID = "EXT-VOTER-123456789";
    private static final PiiEncryptionService ENCRYPTION = new PiiEncryptionService("0123456789abcdef0123456789abcdef".getBytes());

    @LocalServerPort
    int port;

    @MockitoBean
    VoterRollService voterRollService;

    private VoterRecord voter;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/miremsplatform";
        voter = VoterRecord.create(VOTER_ID, EXTERNAL_VOTER_ID, Set.of(ELECTION_ID), RegistrationStatus.ACTIVE, ENCRYPTION);
    }

    @Test
    void electionOfficerCanRegisterVoterAndResponseMasksExternalReference() {
        when(voterRollService.registerVoter(any())).thenReturn(voter);

        given()
                .auth().preemptive().basic("election-officer", "password")
                .header("X-Forwarded-For", "10.10.33.1, 10.10.33.2")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "externalVoterReference": "EXT-VOTER-123456789",
                          "jurisdiction": "US-FED",
                          "eligibleElectionIds": ["20000000-0000-0000-0000-000000000033"]
                        }
                        """)
        .when()
                .post("/voters")
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .header("Location", "/voters/" + VOTER_ID)
                .body("id", equalTo(VOTER_ID.toString()))
                .body("maskedReference", not(containsString(EXTERNAL_VOTER_ID)))
                .body("maskedReference", equalTo("***************6789"))
                .body("registrationStatus", equalTo("ACTIVE"));

        ArgumentCaptor<VoterRollService.RegisterVoterCommand> commandCaptor =
                ArgumentCaptor.forClass(VoterRollService.RegisterVoterCommand.class);
        verify(voterRollService).registerVoter(commandCaptor.capture());
        VoterRollService.RegisterVoterCommand command = commandCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(command.externalVoterId()).isEqualTo(EXTERNAL_VOTER_ID);
        org.assertj.core.api.Assertions.assertThat(command.eligibleElections()).containsExactly(ELECTION_ID);
        org.assertj.core.api.Assertions.assertThat(command.registrationStatus()).isEqualTo(RegistrationStatus.ACTIVE);
        org.assertj.core.api.Assertions.assertThat(command.actorId()).isEqualTo("election-officer");
        org.assertj.core.api.Assertions.assertThat(command.sourceIp()).isEqualTo("10.10.33.1");
    }

    @Test
    void voterCanReadOwnMaskedRecordButNotOtherRecordAndOfficerCanReadAll() {
        when(voterRollService.getVoter(VOTER_ID)).thenReturn(Optional.of(voter));

        given()
                .auth().preemptive().basic(VOTER_ID.toString(), "password")
        .when()
                .get("/voters/{voterId}", VOTER_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(VOTER_ID.toString()))
                .body("maskedReference", equalTo("***************6789"))
                .body("maskedReference", not(containsString(EXTERNAL_VOTER_ID)));

        given()
                .auth().preemptive().basic(OTHER_VOTER_ID.toString(), "password")
        .when()
                .get("/voters/{voterId}", VOTER_ID)
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .auth().preemptive().basic("election-officer", "password")
        .when()
                .get("/voters/{voterId}", VOTER_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(VOTER_ID.toString()));
    }

    @Test
    void voterCanCheckOwnEligibilityAndOfficerCanCheckAll() {
        when(voterRollService.checkEligibility(any()))
                .thenReturn(new VoterEligibilityResult(true, "eligible"));

        given()
                .auth().preemptive().basic(VOTER_ID.toString(), "password")
        .when()
                .get("/voters/{voterId}/eligibility/{electionId}", VOTER_ID, ELECTION_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("voterId", equalTo(VOTER_ID.toString()))
                .body("electionId", equalTo(ELECTION_ID.toString()))
                .body("eligible", equalTo(true))
                .body("reason", equalTo("eligible"));

        given()
                .auth().preemptive().basic(OTHER_VOTER_ID.toString(), "password")
        .when()
                .get("/voters/{voterId}/eligibility/{electionId}", VOTER_ID, ELECTION_ID)
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .auth().preemptive().basic("election-officer", "password")
        .when()
                .get("/voters/{voterId}/eligibility/{electionId}", VOTER_ID, ELECTION_ID)
        .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void voterEndpointsRejectUnauthorizedForbiddenInvalidAndMissingRequests() {
        given()
        .when()
                .get("/voters/{voterId}", VOTER_ID)
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given()
                .auth().preemptive().basic("observer", "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "externalVoterReference": "EXT-VOTER-123456789",
                          "jurisdiction": "US-FED",
                          "eligibleElectionIds": ["20000000-0000-0000-0000-000000000033"]
                        }
                        """)
        .when()
                .post("/voters")
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .auth().preemptive().basic("election-officer", "password")
                .contentType(ContentType.JSON)
                .body("{\"externalVoterReference\":\"\",\"jurisdiction\":\"US-FED\",\"eligibleElectionIds\":[]}")
        .when()
                .post("/voters")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .contentType("application/problem+json");

        when(voterRollService.getVoter(VOTER_ID)).thenReturn(Optional.empty());
        given()
                .auth().preemptive().basic("election-officer", "password")
        .when()
                .get("/voters/{voterId}", VOTER_ID)
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Voter not found"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityUsers {
        @Bean
        PiiEncryptionService testPiiEncryptionService() {
            return ENCRYPTION;
        }

        @Bean
        UserDetailsService testUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("election-officer")
                            .password("{noop}password")
                            .authorities("ROLE_ELECTION_OFFICER", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build(),
                    User.withUsername("observer")
                            .password("{noop}password")
                            .authorities("ROLE_OBSERVER", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build(),
                    User.withUsername(VOTER_ID.toString())
                            .password("{noop}password")
                            .authorities("ROLE_VOTER", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build(),
                    User.withUsername(OTHER_VOTER_ID.toString())
                            .password("{noop}password")
                            .authorities("ROLE_VOTER", "ELECTION_SCOPE_" + ELECTION_ID)
                            .build());
        }
    }
}
