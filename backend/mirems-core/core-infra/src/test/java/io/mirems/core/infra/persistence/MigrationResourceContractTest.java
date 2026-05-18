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
            "db/migration/V5__create_audit_log_table.sql",
            "db/migration/V6__create_tabulation_report_table.sql",
            "db/migration/V7__add_vote_correction_dual_approval_columns.sql",
            "db/migration/V8__add_voting_method_to_voting_sessions.sql",
            "db/migration/V9__create_sample_import_staging_tables.sql");

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
        assertThat(sql).contains("idx_tabulation_reports_election_id");
        assertThat(sql).contains("idx_vote_corrections_first_approved_by");
        assertThat(sql).contains("idx_audit_events_aggregate_id_occurred_at");
        assertThat(sql).contains("idx_sample_precincts_ballot_style_id");
        assertThat(sql).contains("idx_sample_import_batches_bundle_id");
    }

    @Test
    void votingSessionDuplicateVotePartialUniqueIndexIsDeclared() throws IOException {
        String voterMigration = readResource("db/migration/V3__create_voter_tables.sql");
        String votingMethodMigration = readResource("db/migration/V8__add_voting_method_to_voting_sessions.sql");

        assertThat(voterMigration)
                .contains("CREATE UNIQUE INDEX uq_voting_sessions_non_spoiled_per_election")
                .contains("ON voting_sessions (voter_record_id, election_id)")
                .contains("WHERE session_status <> 'SPOILED'");
        assertThat(votingMethodMigration)
                .contains("ALTER TABLE voting_sessions")
                .contains("ADD COLUMN voting_method VARCHAR(64) NOT NULL DEFAULT 'ELECTION_DAY'");
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
