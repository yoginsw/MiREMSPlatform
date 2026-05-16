package io.mirems.extension.us;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsElectoralDistrictTest {
    private static final UUID STATE_ID = UUID.fromString("01900000-0000-7000-8000-200000000001");
    private static final UUID COUNTY_ID = UUID.fromString("01900000-0000-7000-8000-200000000002");
    private static final UUID DISTRICT_ID = UUID.fromString("01900000-0000-7000-8000-200000000003");

    @Test
    void supportsSingleAndMultiMemberDistrictsScopedToJurisdiction() {
        UsJurisdiction california = UsJurisdiction.state(STATE_ID, "06", "California");
        UsJurisdiction alameda = UsJurisdiction.county(COUNTY_ID, "06001", "Alameda County", california);

        UsElectoralDistrict senate = UsElectoralDistrict.singleMember(
                DISTRICT_ID, "CA-SD-09", "California Senate District 9", california);
        UsElectoralDistrict cityCouncil = UsElectoralDistrict.multiMember(
                UUID.fromString("01900000-0000-7000-8000-200000000004"),
                "OAK-CC-AT-LARGE",
                "Oakland City Council At-Large",
                alameda,
                3);

        assertEquals(1, senate.seats());
        assertEquals(3, cityCouncil.seats());
        assertTrue(cityCouncil.isMultiMember());
        assertEquals(UsDistrictType.STATE_LEGISLATURE, senate.type());
        assertEquals(UsDistrictType.LOCAL_AT_LARGE, cityCouncil.type());
        assertSame(california, senate.jurisdiction());
        assertSame(alameda, cityCouncil.jurisdiction());
    }

    @Test
    void rejectsInvalidDistrictCodesAndSeatCounts() {
        UsJurisdiction california = UsJurisdiction.state(STATE_ID, "06", "California");

        assertThrows(IllegalArgumentException.class, () -> UsElectoralDistrict.singleMember(DISTRICT_ID, " ", "District", california));
        assertThrows(IllegalArgumentException.class, () -> UsElectoralDistrict.singleMember(DISTRICT_ID, "CA-SD-09", " ", california));
        assertThrows(IllegalArgumentException.class, () -> UsElectoralDistrict.singleMember(DISTRICT_ID, "CA-SD-09", "District", null));

        IllegalArgumentException singleMemberException = assertThrows(
                IllegalArgumentException.class,
                () -> UsElectoralDistrict.multiMember(DISTRICT_ID, "CA-AD-01", "Assembly District", california, 1));
        assertTrue(singleMemberException.getMessage().contains("multi-member district must have at least 2 seats"));

        IllegalArgumentException tooManySeatsException = assertThrows(
                IllegalArgumentException.class,
                () -> UsElectoralDistrict.multiMember(DISTRICT_ID, "CA-AD-01", "Assembly District", california, 100));
        assertTrue(tooManySeatsException.getMessage().contains("district seats must not exceed 99"));
    }
}
