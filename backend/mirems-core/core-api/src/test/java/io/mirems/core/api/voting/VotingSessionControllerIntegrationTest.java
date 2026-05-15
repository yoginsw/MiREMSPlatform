package io.mirems.core.api.voting;

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
import io.mirems.core.domain.voting.RegistrationStatus;
import io.mirems.core.domain.voting.VoterRecord;
import io.mirems.core.domain.voting.VotingSession;
import io.mirems.core.domain.voting.VotingSessionValidationException;
import io.mirems.core.domain.voting.encryption.PiiEncryptionService;
import io.mirems.core.infra.service.voting.VotingSessionService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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
        classes = {MiremsCoreApiApplication.class, VotingSessionControllerIntegrationTest.TestSecurityUsers.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
class VotingSessionControllerIntegrationTest {
    private static final UUID SESSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000034");
    private static final UUID VOTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000035");
    private static final UUID OTHER_VOTER_ID = UUID.fromString("30000000-0000-0000-0000-000000000036");
    private static final UUID ELECTION_ID = UUID.fromString("30000000-0000-0000-0000-000000000037");
    private static final UUID BALLOT_STYLE_ID = UUID.fromString("30000000-0000-0000-0000-000000000038");
    private static final UUID CONTEST_ID = UUID.fromString("30000000-0000-0000-0000-000000000039");
    private static final UUID CANDIDATE_ID = UUID.fromString("30000000-0000-0000-0000-000000000040");
    private static final String RESULT_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @LocalServerPort
    int port;

    @MockitoBean
    VotingSessionService votingSessionService;

    private VotingSession session;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/miremsplatform";
        session = openedSession();
        when(votingSessionService.electionIdForSession(SESSION_ID)).thenReturn(ELECTION_ID);
    }

    @Test
    void voterCanCreateSessionCastVoteAndSpoilOwnSession() {
        when(votingSessionService.openSession(any())).thenReturn(session);
        when(votingSessionService.castBallot(any()))
                .thenReturn(new VotingSessionService.CastBallotReceipt(SESSION_ID, List.of(RESULT_HASH)));
        VotingSession spoiledSession = openedSession();
        spoiledSession.spoil(OffsetDateTime.parse("2026-06-03T09:05:00Z"));
        when(votingSessionService.spoilBallot(SESSION_ID, VOTER_ID.toString(), "10.10.34.1")).thenReturn(spoiledSession);

        given()
                .auth().preemptive().basic(VOTER_ID.toString(), "password")
                .header("X-Forwarded-For", "10.10.34.1")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "voterId": "30000000-0000-0000-0000-000000000035",
                          "electionId": "30000000-0000-0000-0000-000000000037",
                          "ballotStyleId": "30000000-0000-0000-0000-000000000038",
                          "deviceId": "kiosk-034"
                        }
                        """)
        .when()
                .post("/sessions")
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .header("Location", "/sessions/" + SESSION_ID)
                .body("id", equalTo(SESSION_ID.toString()))
                .body("voterId", equalTo(VOTER_ID.toString()))
                .body("electionId", equalTo(ELECTION_ID.toString()))
                .body("ballotStyleId", equalTo(BALLOT_STYLE_ID.toString()))
                .body("status", equalTo("OPENED"));

        ArgumentCaptor<VotingSessionService.OpenSessionCommand> openCaptor =
                ArgumentCaptor.forClass(VotingSessionService.OpenSessionCommand.class);
        verify(votingSessionService).openSession(openCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(openCaptor.getValue().deviceId()).isEqualTo("kiosk-034");
        org.assertj.core.api.Assertions.assertThat(openCaptor.getValue().sourceIp()).isEqualTo("10.10.34.1");

        given()
                .auth().preemptive().basic(VOTER_ID.toString(), "password")
                .header("X-Forwarded-For", "10.10.34.1")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "selections": [
                            {
                              "contestId": "30000000-0000-0000-0000-000000000039",
                              "selectionIds": ["30000000-0000-0000-0000-000000000040"]
                            }
                          ]
                        }
                        """)
        .when()
                .post("/sessions/{sessionId}/cast", SESSION_ID)
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("sessionId", equalTo(SESSION_ID.toString()))
                .body("resultHashes", hasSize(1))
                .body("resultHashes[0]", equalTo(RESULT_HASH))
                .body("receiptHash", equalTo(RESULT_HASH));

        ArgumentCaptor<VotingSessionService.CastBallotCommand> castCaptor =
                ArgumentCaptor.forClass(VotingSessionService.CastBallotCommand.class);
        verify(votingSessionService).castBallot(castCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(castCaptor.getValue().selections()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(castCaptor.getValue().selections().getFirst().contestId()).isEqualTo(CONTEST_ID);
        org.assertj.core.api.Assertions.assertThat(castCaptor.getValue().selections().getFirst().selectedCandidateIds())
                .containsExactly(CANDIDATE_ID);

        given()
                .auth().preemptive().basic(VOTER_ID.toString(), "password")
                .header("X-Forwarded-For", "10.10.34.1")
        .when()
                .post("/sessions/{sessionId}/spoil", SESSION_ID)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("SPOILED"));
    }

    @Test
    void duplicateCastReturnsConflictProblemDetail() {
        when(votingSessionService.castBallot(any()))
                .thenThrow(new VotingSessionValidationException("VotingSession must be OPENED before casting a ballot"));

        given()
                .auth().preemptive().basic(VOTER_ID.toString(), "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "selections": [
                            {
                              "contestId": "30000000-0000-0000-0000-000000000039",
                              "selectionIds": ["30000000-0000-0000-0000-000000000040"]
                            }
                          ]
                        }
                        """)
        .when()
                .post("/sessions/{sessionId}/cast", SESSION_ID)
        .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Voting session conflict"));
    }

    @Test
    void votingSessionEndpointsRejectUnauthorizedForbiddenInvalidAndMissingRequests() {
        given()
        .when()
                .post("/sessions/{sessionId}/spoil", SESSION_ID)
        .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        given()
                .auth().preemptive().basic(OTHER_VOTER_ID.toString(), "password")
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "voterId": "30000000-0000-0000-0000-000000000035",
                          "electionId": "30000000-0000-0000-0000-000000000037",
                          "ballotStyleId": "30000000-0000-0000-0000-000000000038",
                          "deviceId": "kiosk-034"
                        }
                        """)
        .when()
                .post("/sessions")
        .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given()
                .auth().preemptive().basic(VOTER_ID.toString(), "password")
                .contentType(ContentType.JSON)
                .body("{\"voterId\":null,\"electionId\":null,\"ballotStyleId\":null,\"deviceId\":\"\"}")
        .when()
                .post("/sessions")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .contentType("application/problem+json");

        when(votingSessionService.spoilBallot(SESSION_ID, "election-officer", "127.0.0.1"))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("VotingSession not found: " + SESSION_ID));
        given()
                .auth().preemptive().basic("election-officer", "password")
        .when()
                .post("/sessions/{sessionId}/spoil", SESSION_ID)
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .contentType("application/problem+json")
                .body("title", equalTo("Voting session not found"));
    }

    private VotingSession openedSession() {
        Election election = Election.create(ELECTION_ID, "Election", ElectionType.LOCAL, "Seoul", LocalDate.of(2026, 6, 3), "KR", "ext-kr");
        Ballot ballot = Ballot.create(UUID.fromString("30000000-0000-0000-0000-000000000041"), election);
        BallotStyle ballotStyle = ballot.addStyle(BALLOT_STYLE_ID, "SEOUL-034", "Seoul", "ko", Set.of(AccessibilityFeature.LARGE_TEXT));
        VoterRecord voter = VoterRecord.create(VOTER_ID, "EXT-VOTER-034", Set.of(ELECTION_ID), RegistrationStatus.ACTIVE,
                new PiiEncryptionService("0123456789abcdef0123456789abcdef".getBytes()));
        Contest.create(CONTEST_ID, election, ContestType.CANDIDATE_CHOICE, "Mayor", 1, 1);
        return voter.openVotingSession(SESSION_ID, election, ballotStyle, "kiosk-034", OffsetDateTime.parse("2026-06-03T09:00:00Z"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSecurityUsers {
        @Bean
        UserDetailsService testUsers() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("election-officer")
                            .password("{noop}password")
                            .authorities("ROLE_ELECTION_OFFICER", "ELECTION_SCOPE_" + ELECTION_ID)
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
