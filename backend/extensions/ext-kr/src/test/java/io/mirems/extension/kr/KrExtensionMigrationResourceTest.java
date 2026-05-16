package io.mirems.extension.kr;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class KrExtensionMigrationResourceTest {
    @Test
    void krExtensionMigrationResourceIsPresentWithScopedTablesAndIndexes() throws Exception {
        String migrationPath = "db/migration/ext/kr/V100__kr_extension_tables.sql";

        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(migrationPath)) {
            assertThat(input).as(migrationPath).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(sql)
                    .contains("CREATE TABLE kr_extension_metadata")
                    .contains("CREATE TABLE kr_election_type_mappings")
                    .contains("CREATE INDEX idx_kr_election_type_mappings_election_id")
                    .contains("extension_pack_id VARCHAR(32) NOT NULL DEFAULT 'kr'");
        }
    }
}
