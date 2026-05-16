package io.mirems.extension.us;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class UsJurisdictionPostgresMigrationTest {
    private static final UUID STATE_ID = UUID.fromString("01900000-0000-7000-8000-300000000001");
    private static final UUID COUNTY_ID = UUID.fromString("01900000-0000-7000-8000-300000000002");
    private static final UUID PRECINCT_1_ID = UUID.fromString("01900000-0000-7000-8000-300000000003");
    private static final UUID PRECINCT_2_ID = UUID.fromString("01900000-0000-7000-8000-300000000004");
    private static final UUID DISTRICT_ID = UUID.fromString("01900000-0000-7000-8000-300000000005");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4")
            .withDatabaseName("mirems_ext_us_test")
            .withUsername("mirems")
            .withPassword("mirems");

    @Test
    void migrationEnforcesHierarchyFipsMappingAndDistrictSeatRules() throws Exception {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            createCoreElectionStub(connection);
            Flyway.configure()
                    .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                    .locations("classpath:db/migration/ext/us")
                    .load()
                    .migrate();

            execute(connection, """
                    INSERT INTO us_jurisdictions (id, fips_code, name, level)
                    VALUES (?, '06', 'California', 'STATE')
                    """, STATE_ID);
            execute(connection, """
                    INSERT INTO us_jurisdictions (id, parent_jurisdiction_id, fips_code, name, level)
                    VALUES (?, ?, '06001', 'Alameda County', 'COUNTY')
                    """, COUNTY_ID, STATE_ID);
            execute(connection, """
                    INSERT INTO us_jurisdictions (id, parent_jurisdiction_id, fips_code, precinct_code, name, level)
                    VALUES (?, ?, '06001', 'PR-00042', 'Oakland Precinct 42', 'PRECINCT')
                    """, PRECINCT_1_ID, COUNTY_ID);
            execute(connection, """
                    INSERT INTO us_jurisdictions (id, parent_jurisdiction_id, fips_code, precinct_code, name, level)
                    VALUES (?, ?, '06001', 'PR-00043', 'Oakland Precinct 43', 'PRECINCT')
                    """, PRECINCT_2_ID, COUNTY_ID);
            execute(connection, """
                    INSERT INTO us_electoral_districts (id, jurisdiction_id, district_code, name, district_type, seat_count)
                    VALUES (?, ?, 'OAK-CC-AT-LARGE', 'Oakland City Council At-Large', 'LOCAL_AT_LARGE', 3)
                    """, DISTRICT_ID, COUNTY_ID);

            assertThat(countRows(connection, "us_jurisdictions")).isEqualTo(4);
            assertThat(countRows(connection, "us_electoral_districts")).isEqualTo(1);

            assertThatSqlFails(connection, """
                    INSERT INTO us_jurisdictions (id, parent_jurisdiction_id, fips_code, name, level)
                    VALUES ('01900000-0000-7000-8000-300000000006', ?, '36061', 'New York County', 'COUNTY')
                    """, STATE_ID).hasMessageContaining("county FIPS must start with parent state FIPS");
            assertThatSqlFails(connection, """
                    INSERT INTO us_jurisdictions (id, parent_jurisdiction_id, fips_code, precinct_code, name, level)
                    VALUES ('01900000-0000-7000-8000-300000000007', ?, '06013', 'PR-00044', 'Contra Costa Precinct', 'PRECINCT')
                    """, COUNTY_ID).hasMessageContaining("precinct mapping must start with parent county FIPS");
            assertThatSqlFails(connection, "UPDATE us_jurisdictions SET level = 'COUNTY' WHERE id = ?", STATE_ID)
                    .hasMessageContaining("US jurisdiction level is immutable");
            assertThatSqlFails(connection, "UPDATE us_jurisdictions SET fips_code = '07' WHERE id = ?", STATE_ID)
                    .hasMessageContaining("US jurisdiction FIPS is immutable");
            assertThatSqlFails(connection, """
                    INSERT INTO us_electoral_districts (id, jurisdiction_id, district_code, name, district_type, seat_count)
                    VALUES ('01900000-0000-7000-8000-300000000008', ?, 'OAK-INVALID', 'Invalid At-Large', 'LOCAL_AT_LARGE', 1)
                    """, COUNTY_ID).hasMessageContaining("chk_us_electoral_districts_type_seat_count");
        }
    }

    private static void createCoreElectionStub(java.sql.Connection connection) throws SQLException {
        execute(connection, "CREATE TABLE elections (id UUID PRIMARY KEY)");
    }

    private static int countRows(java.sql.Connection connection, String tableName) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static AbstractThrowableAssertAdapter assertThatSqlFails(
            java.sql.Connection connection, String sql, Object... parameters) {
        AbstractThrowableAssertAdapter adapter = new AbstractThrowableAssertAdapter();
        assertThatThrownBy(() -> execute(connection, sql, parameters)).satisfies(adapter::capture);
        return adapter;
    }

    private static void execute(java.sql.Connection connection, String sql, Object... parameters) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            statement.execute();
        }
    }

    private static final class AbstractThrowableAssertAdapter {
        private Throwable throwable;

        void capture(Throwable throwable) {
            this.throwable = throwable;
        }

        AbstractThrowableAssertAdapter hasMessageContaining(String expected) {
            assertThat(throwable).hasMessageContaining(expected);
            return this;
        }
    }
}
