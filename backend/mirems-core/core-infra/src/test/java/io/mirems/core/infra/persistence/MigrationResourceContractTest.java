package io.mirems.core.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationResourceContractTest {
    private static final List<String> REQUIRED_MIGRATIONS = List.of(
            "db/migration/V1__create_election_tables.sql",
            "db/migration/V2__create_ballot_tables.sql",
            "db/migration/V3__create_voter_tables.sql",
            "db/migration/V4__create_voting_result_tables.sql",
            "db/migration/V5__create_audit_log_table.sql");

    @Test
    void requiredFlywayMigrationsArePresent() {
        for (String migration : REQUIRED_MIGRATIONS) {
            assertThat(Thread.currentThread().getContextClassLoader().getResource(migration))
                    .as(migration)
                    .isNotNull();
        }
    }

    @Test
    void requiredIndexesAreDeclaredInMigrations() throws IOException {
        String sql = String.join("\n", REQUIRED_MIGRATIONS.stream()
                .map(MigrationResourceContractTest::readResource)
                .toList());

        assertThat(sql).contains("idx_elections_status");
        assertThat(sql).contains("idx_voting_results_session_id");
        assertThat(sql).contains("idx_audit_events_aggregate_id_occurred_at");
    }

    @Test
    void votingSessionDuplicateVotePartialUniqueIndexIsDeclared() throws IOException {
        String voterMigration = readResource("db/migration/V3__create_voter_tables.sql");

        assertThat(voterMigration)
                .contains("uq_voting_sessions_non_spoiled_per_election")
                .contains("ON voting_sessions (voter_record_id, election_id)")
                .contains("WHERE session_status <> 'SPOILED'");
    }

    private static String readResource(String path) {
        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read resource: " + path, exception);
        }
    }
}
