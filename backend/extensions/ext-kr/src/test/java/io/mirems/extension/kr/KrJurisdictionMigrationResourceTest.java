package io.mirems.extension.kr;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class KrJurisdictionMigrationResourceTest {
    @Test
    void containsKrJurisdictionMigrationWithHierarchyAndElectionTypeTables() throws Exception {
        String migrationPath = "/db/migration/ext/kr/V101__kr_election_type_jurisdiction_tables.sql";

        try (var input = getClass().getResourceAsStream(migrationPath)) {
            assertNotNull(input, migrationPath);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(sql.contains("CREATE TABLE kr_election_types"));
            assertTrue(sql.contains("PRESIDENTIAL_ELECTION"));
            assertTrue(sql.contains("NATIONAL_ASSEMBLY_ELECTION"));
            assertTrue(sql.contains("LOCAL_ELECTION"));
            assertTrue(sql.contains("SUPERINTENDENT_ELECTION"));
            assertTrue(sql.contains("BY_ELECTION"));
            assertTrue(sql.contains("CREATE TABLE kr_jurisdictions"));
            assertTrue(sql.contains("parent_jurisdiction_id UUID REFERENCES kr_jurisdictions(id)"));
            assertTrue(sql.contains("constituency_code VARCHAR(32) NOT NULL"));
            assertTrue(sql.contains("chk_kr_jurisdictions_level"));
            assertTrue(sql.contains("validate_kr_jurisdiction_parent_level"));
            assertTrue(sql.contains("KR jurisdiction level is immutable"));
            assertTrue(sql.contains("EUPMYEONDONG parent must be SIGUNGU"));
            assertTrue(sql.contains("SIGUNGU parent must be SIDO"));
            assertTrue(sql.contains("CREATE TRIGGER trg_kr_jurisdictions_parent_level"));
            assertTrue(sql.contains("idx_kr_jurisdictions_parent"));
            assertTrue(sql.contains("idx_kr_jurisdictions_constituency_code"));
        }
    }
}
