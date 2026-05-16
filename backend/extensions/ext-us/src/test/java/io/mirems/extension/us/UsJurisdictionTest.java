package io.mirems.extension.us;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsJurisdictionTest {
    private static final UUID STATE_ID = UUID.fromString("01900000-0000-7000-8000-100000000001");
    private static final UUID COUNTY_ID = UUID.fromString("01900000-0000-7000-8000-100000000002");
    private static final UUID PRECINCT_ID = UUID.fromString("01900000-0000-7000-8000-100000000003");

    @Test
    void buildsStateCountyPrecinctHierarchyWithFipsAndPrecinctCodeMapping() {
        UsJurisdiction california = UsJurisdiction.state(STATE_ID, "06", "California");
        UsJurisdiction alameda = UsJurisdiction.county(COUNTY_ID, "06001", "Alameda County", california);
        UsJurisdiction precinct = UsJurisdiction.precinct(PRECINCT_ID, "06001-PR-00042", "Oakland Precinct 42", alameda);

        assertEquals(UsJurisdictionLevel.STATE, california.level());
        assertEquals(UsJurisdictionLevel.COUNTY, alameda.level());
        assertEquals(UsJurisdictionLevel.PRECINCT, precinct.level());
        assertEquals("06", california.fipsCode());
        assertEquals("06001", alameda.fipsCode());
        assertEquals("06001", precinct.countyFipsCode());
        assertEquals("PR-00042", precinct.precinctCode());
        assertSame(california, alameda.parent().orElseThrow());
        assertSame(alameda, precinct.parent().orElseThrow());
        assertEquals("California > Alameda County > Oakland Precinct 42", precinct.hierarchyPath());
    }

    @Test
    void rejectsInvalidParentLevelsAndMismatchedFipsHierarchy() {
        UsJurisdiction california = UsJurisdiction.state(STATE_ID, "06", "California");
        UsJurisdiction alameda = UsJurisdiction.county(COUNTY_ID, "06001", "Alameda County", california);

        IllegalArgumentException countyParentException = assertThrows(
                IllegalArgumentException.class,
                () -> UsJurisdiction.county(COUNTY_ID, "06001", "Alameda County", null));
        assertTrue(countyParentException.getMessage().contains("COUNTY parent must be STATE"));

        IllegalArgumentException precinctParentException = assertThrows(
                IllegalArgumentException.class,
                () -> UsJurisdiction.precinct(PRECINCT_ID, "06001-PR-00042", "Oakland Precinct 42", california));
        assertTrue(precinctParentException.getMessage().contains("PRECINCT parent must be COUNTY"));

        IllegalArgumentException mismatchedCountyException = assertThrows(
                IllegalArgumentException.class,
                () -> UsJurisdiction.county(COUNTY_ID, "36061", "New York County", california));
        assertTrue(mismatchedCountyException.getMessage().contains("county FIPS must start with parent state FIPS"));

        IllegalArgumentException mismatchedPrecinctException = assertThrows(
                IllegalArgumentException.class,
                () -> UsJurisdiction.precinct(PRECINCT_ID, "06013-PR-00042", "Contra Costa Precinct", alameda));
        assertTrue(mismatchedPrecinctException.getMessage().contains("precinct mapping must start with parent county FIPS"));
    }

    @Test
    void normalizesNamesAndRejectsInvalidFipsAndPrecinctCodes() {
        UsJurisdiction california = UsJurisdiction.state(STATE_ID, " 06 ", " California ");

        assertEquals("06", california.fipsCode());
        assertEquals("California", california.name());

        assertThrows(IllegalArgumentException.class, () -> UsJurisdiction.state(STATE_ID, "6", "California"));
        assertThrows(IllegalArgumentException.class, () -> UsJurisdiction.state(STATE_ID, "CA", "California"));
        assertThrows(IllegalArgumentException.class, () -> UsJurisdiction.county(COUNTY_ID, "0601", "Alameda County", california));
        assertThrows(IllegalArgumentException.class, () -> UsJurisdiction.precinct(PRECINCT_ID, "06001", "Oakland", UsJurisdiction.county(COUNTY_ID, "06001", "Alameda County", california)));
        assertThrows(IllegalArgumentException.class, () -> UsJurisdiction.state(STATE_ID, "06", " "));
    }
}
