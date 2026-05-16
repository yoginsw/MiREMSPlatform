package io.mirems.extension.us;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UsExtensionMigrationResourceTest {
    @Test
    void usExtensionMigrationResourceIsPresentWithScopedTablesAndIndexes() throws Exception {
        String migrationPath = "db/migration/ext/us/V100__us_extension_tables.sql";

        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(migrationPath)) {
            assertThat(input).as(migrationPath).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE us_extension_metadata")
                    .contains("CREATE TABLE us_election_type_mappings")
                    .contains("CREATE INDEX idx_us_election_type_mappings_election_id")
                    .contains("extension_pack_id VARCHAR(32) NOT NULL DEFAULT 'us'");
        }
    }
}
