package io.mirems.extension.us;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class UsJurisdictionMigrationResourceTest {
    @Test
    void containsUsJurisdictionMigrationWithFipsPrecinctAndMultiMemberDistrictTables() throws Exception {
        String migrationPath = "/db/migration/ext/us/V101__us_jurisdiction_precinct_district_tables.sql";

        try (var input = getClass().getResourceAsStream(migrationPath)) {
            assertNotNull(input, migrationPath);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(sql.contains("CREATE TABLE us_jurisdictions"));
            assertTrue(sql.contains("STATE"));
            assertTrue(sql.contains("COUNTY"));
            assertTrue(sql.contains("PRECINCT"));
            assertTrue(sql.contains("fips_code VARCHAR(32) NOT NULL"));
            assertTrue(sql.contains("uq_us_jurisdictions_state_fips"));
            assertTrue(sql.contains("uq_us_jurisdictions_county_fips"));
            assertTrue(sql.contains("uq_us_jurisdictions_precinct_mapping"));
            assertTrue(sql.contains("US jurisdiction FIPS is immutable"));
            assertTrue(sql.contains("precinct_code VARCHAR(64)"));
            assertTrue(sql.contains("validate_us_jurisdiction_parent_level"));
            assertTrue(sql.contains("US jurisdiction level is immutable"));
            assertTrue(sql.contains("COUNTY parent must be STATE"));
            assertTrue(sql.contains("PRECINCT parent must be COUNTY"));
            assertTrue(sql.contains("CREATE TABLE us_electoral_districts"));
            assertTrue(sql.contains("seat_count INTEGER NOT NULL"));
            assertTrue(sql.contains("chk_us_electoral_districts_type_seat_count"));
            assertTrue(sql.contains("idx_us_jurisdictions_parent"));
            assertTrue(sql.contains("idx_us_jurisdictions_fips_code"));
            assertTrue(sql.contains("idx_us_electoral_districts_jurisdiction"));
        }
    }
}
