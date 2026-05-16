package io.mirems.extension.kr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class KrJurisdictionTest {
    private static final UUID SIDO_ID = UUID.fromString("01900000-0000-7000-8000-000000000001");
    private static final UUID SIGUNGU_ID = UUID.fromString("01900000-0000-7000-8000-000000000002");
    private static final UUID EUPMYEONDONG_ID = UUID.fromString("01900000-0000-7000-8000-000000000003");

    @Test
    void buildsThreeLevelKoreanJurisdictionHierarchy() {
        KrJurisdiction seoul = KrJurisdiction.sido(SIDO_ID, "11", "서울특별시", "1100000");
        KrJurisdiction jongno = KrJurisdiction.sigungu(SIGUNGU_ID, "11110", "종로구", "1111000", seoul);
        KrJurisdiction cheongun = KrJurisdiction.eupMyeonDong(EUPMYEONDONG_ID, "11110515", "청운효자동", "11110515", jongno);

        assertEquals(KrJurisdictionLevel.SIDO, seoul.level());
        assertEquals(KrJurisdictionLevel.SIGUNGU, jongno.level());
        assertEquals(KrJurisdictionLevel.EUPMYEONDONG, cheongun.level());
        assertSame(seoul, jongno.parent().orElseThrow());
        assertSame(jongno, cheongun.parent().orElseThrow());
        assertEquals("서울특별시 > 종로구 > 청운효자동", cheongun.hierarchyPath());
        assertEquals("11110515", cheongun.constituencyCode());
    }

    @Test
    void rejectsInvalidParentLevelForHierarchy() {
        KrJurisdiction seoul = KrJurisdiction.sido(SIDO_ID, "11", "서울특별시", "1100000");

        IllegalArgumentException eupParentException = assertThrows(
                IllegalArgumentException.class,
                () -> KrJurisdiction.eupMyeonDong(EUPMYEONDONG_ID, "11110515", "청운효자동", "11110515", seoul));
        assertTrue(eupParentException.getMessage().contains("EUPMYEONDONG parent must be SIGUNGU"));

        IllegalArgumentException sigunguParentException = assertThrows(
                IllegalArgumentException.class,
                () -> KrJurisdiction.sigungu(SIGUNGU_ID, "11110", "종로구", "1111000", null));
        assertTrue(sigunguParentException.getMessage().contains("SIGUNGU parent must be SIDO"));
    }

    @Test
    void normalizesCodesAndRejectsBlankRequiredFields() {
        KrJurisdiction seoul = KrJurisdiction.sido(SIDO_ID, " 11 ", " 서울특별시 ", " 1100000 ");

        assertEquals("11", seoul.administrativeCode());
        assertEquals("서울특별시", seoul.name());
        assertEquals("1100000", seoul.constituencyCode());

        assertThrows(IllegalArgumentException.class, () -> KrJurisdiction.sido(SIDO_ID, " ", "서울특별시", "1100000"));
        assertThrows(IllegalArgumentException.class, () -> KrJurisdiction.sido(SIDO_ID, "11", " ", "1100000"));
        assertThrows(IllegalArgumentException.class, () -> KrJurisdiction.sido(SIDO_ID, "11", "서울특별시", " "));
    }
}
