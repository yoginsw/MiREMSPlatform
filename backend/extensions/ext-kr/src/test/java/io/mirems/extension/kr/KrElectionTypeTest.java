package io.mirems.extension.kr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mirems.core.domain.election.ElectionType;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class KrElectionTypeTest {
    @Test
    void exposesKoreanPublicOfficialElectionTypes() {
        Set<KrElectionType> electionTypes = Stream.of(KrElectionType.values()).collect(Collectors.toUnmodifiableSet());

        assertTrue(electionTypes.contains(KrElectionType.PRESIDENTIAL_ELECTION));
        assertTrue(electionTypes.contains(KrElectionType.NATIONAL_ASSEMBLY_ELECTION));
        assertTrue(electionTypes.contains(KrElectionType.LOCAL_ELECTION));
        assertTrue(electionTypes.contains(KrElectionType.SUPERINTENDENT_ELECTION));
        assertTrue(electionTypes.contains(KrElectionType.BY_ELECTION));
        assertEquals(5, electionTypes.size());
    }

    @Test
    void mapsKoreanElectionTypesToCoreElectionCategoriesAndLabels() {
        assertEquals(ElectionType.PRESIDENTIAL, KrElectionType.PRESIDENTIAL_ELECTION.coreElectionType());
        assertEquals("대통령선거", KrElectionType.PRESIDENTIAL_ELECTION.koreanLabel());
        assertEquals("presidential-election", KrElectionType.PRESIDENTIAL_ELECTION.slug());

        assertEquals(ElectionType.PARLIAMENTARY, KrElectionType.NATIONAL_ASSEMBLY_ELECTION.coreElectionType());
        assertEquals("국회의원선거", KrElectionType.NATIONAL_ASSEMBLY_ELECTION.koreanLabel());

        assertEquals(ElectionType.LOCAL, KrElectionType.LOCAL_ELECTION.coreElectionType());
        assertEquals("지방선거", KrElectionType.LOCAL_ELECTION.koreanLabel());

        assertEquals(ElectionType.LOCAL, KrElectionType.SUPERINTENDENT_ELECTION.coreElectionType());
        assertEquals("교육감선거", KrElectionType.SUPERINTENDENT_ELECTION.koreanLabel());

        assertEquals(ElectionType.REGIONAL, KrElectionType.BY_ELECTION.coreElectionType());
        assertEquals("보궐선거", KrElectionType.BY_ELECTION.koreanLabel());
    }
}
