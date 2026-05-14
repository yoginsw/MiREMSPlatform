package io.mirems.core.infra.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class VotingSessionDuplicateConstraintPostgresTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4")
            .withDatabaseName("mirems")
            .withUsername("mirems")
            .withPassword("mirems");

    @Test
    void postgresRejectsSecondNonSpoiledVotingSessionForSameVoterAndElection() throws SQLException {
        migrate();
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            insertReferenceRows(statement);
            statement.executeUpdate("""
                    INSERT INTO voting_sessions
                    (id, voter_record_id, election_id, ballot_style_id, started_at, session_status, device_id)
                    VALUES
                    ('018f4b82-aaaa-7aaa-8aaa-aaaaaaaaaaaa',
                     '018f4b82-bbbb-7bbb-8bbb-bbbbbbbbbbbb',
                     '018f4b82-cccc-7ccc-8ccc-cccccccccccc',
                     '018f4b82-dddd-7ddd-8ddd-dddddddddddd',
                     NOW(), 'OPENED', 'kiosk-01')
                    """);

            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO voting_sessions
                    (id, voter_record_id, election_id, ballot_style_id, started_at, session_status, device_id)
                    VALUES
                    ('018f4b82-eeee-7eee-8eee-eeeeeeeeeeee',
                     '018f4b82-bbbb-7bbb-8bbb-bbbbbbbbbbbb',
                     '018f4b82-cccc-7ccc-8ccc-cccccccccccc',
                     '018f4b82-dddd-7ddd-8ddd-dddddddddddd',
                     NOW(), 'OPENED', 'kiosk-02')
                    """))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("uq_voting_sessions_non_spoiled_per_election");
        }
    }

    @Test
    void postgresAllowsReplacementSessionAfterPriorSessionIsSpoiled() throws SQLException {
        migrate();
        try (var connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE voting_sessions, voter_records, ballot_styles, ballots, contests, elections CASCADE");
            insertReferenceRows(statement);
            statement.executeUpdate("""
                    INSERT INTO voting_sessions
                    (id, voter_record_id, election_id, ballot_style_id, started_at, completed_at, session_status, device_id)
                    VALUES
                    ('018f4b82-aaaa-7aaa-8aaa-aaaaaaaaaaaa',
                     '018f4b82-bbbb-7bbb-8bbb-bbbbbbbbbbbb',
                     '018f4b82-cccc-7ccc-8ccc-cccccccccccc',
                     '018f4b82-dddd-7ddd-8ddd-dddddddddddd',
                     NOW(), NOW(), 'SPOILED', 'kiosk-01')
                    """);

            statement.executeUpdate("""
                    INSERT INTO voting_sessions
                    (id, voter_record_id, election_id, ballot_style_id, started_at, session_status, device_id)
                    VALUES
                    ('018f4b82-eeee-7eee-8eee-eeeeeeeeeeee',
                     '018f4b82-bbbb-7bbb-8bbb-bbbbbbbbbbbb',
                     '018f4b82-cccc-7ccc-8ccc-cccccccccccc',
                     '018f4b82-dddd-7ddd-8ddd-dddddddddddd',
                     NOW(), 'OPENED', 'kiosk-02')
                    """);
        }
    }

    private static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private static void insertReferenceRows(Statement statement) throws SQLException {
        statement.executeUpdate("""
                INSERT INTO elections
                (id, name, election_type, jurisdiction, scheduled_date, country_code, extension_pack_id, election_status)
                VALUES
                ('018f4b82-cccc-7ccc-8ccc-cccccccccccc', 'Local Election', 'LOCAL', 'Seoul', DATE '2026-06-03', 'KR', 'ext-kr', 'ACTIVE')
                """);
        statement.executeUpdate("""
                INSERT INTO contests
                (id, election_id, contest_type, name, seats, vote_limit)
                VALUES
                ('018f4b82-ffff-7fff-8fff-ffffffffffff', '018f4b82-cccc-7ccc-8ccc-cccccccccccc', 'CANDIDATE_CHOICE', 'Mayor', 1, 1)
                """);
        statement.executeUpdate("""
                INSERT INTO ballots
                (id, election_id, ballot_version, active)
                VALUES
                ('018f4b82-9999-7999-8999-999999999999', '018f4b82-cccc-7ccc-8ccc-cccccccccccc', 1, TRUE)
                """);
        statement.executeUpdate("""
                INSERT INTO ballot_styles
                (id, ballot_id, style_code, district, language, accessibility_features)
                VALUES
                ('018f4b82-dddd-7ddd-8ddd-dddddddddddd', '018f4b82-9999-7999-8999-999999999999', 'SEOUL-01', 'Seoul', 'ko', '[]'::jsonb)
                """);
        statement.executeUpdate("""
                INSERT INTO voter_records
                (id, encrypted_external_voter_id, eligible_elections, registration_status)
                VALUES
                ('018f4b82-bbbb-7bbb-8bbb-bbbbbbbbbbbb', 'encrypted-payload', '["018f4b82-cccc-7ccc-8ccc-cccccccccccc"]'::jsonb, 'ACTIVE')
                """);
    }
}
