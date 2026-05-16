package io.mirems.extension.kr.rules;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.extension.kr.KrElectionType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

class KrVoterEligibilityDecisionServiceTest {
    private static final Path DMN = Path.of("src/main/resources/decisions/kr/KrVoterEligibility.dmn");

    private final KrVoterEligibilityDecisionService service = new KrVoterEligibilityDecisionService();

    @ParameterizedTest(name = "{0}")
    @MethodSource("decisionRows")
    void evaluatesEveryKrVoterEligibilityDecisionRow(
            String scenario,
            KrVoterEligibilityRequest request,
            boolean expectedEligible,
            String expectedReason) {
        KrVoterEligibilityResult result = service.evaluate(request);

        assertThat(result.eligible()).as(scenario).isEqualTo(expectedEligible);
        assertThat(result.reason()).isEqualTo(expectedReason);
    }

    @Test
    void voterEligibilityDmnDeclaresInputsOutputsAndDecisionRows() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());
        String xml = Files.readString(DMN);

        assertThat(xml).contains("name=\"voterAge\"");
        assertThat(xml).contains("name=\"citizenship\"");
        assertThat(xml).contains("name=\"permanentResident\"");
        assertThat(xml).contains("name=\"electionType\"");
        assertThat(xml).contains("name=\"eligible\"");
        assertThat(xml).contains("name=\"reason\"");
        assertThat(document.getElementsByTagNameNS("*", "rule").getLength()).isEqualTo(10);
    }

    private static Stream<Arguments> decisionRows() {
        return Stream.of(
                Arguments.of(
                        "under 18 voters are ineligible",
                        request(17, KrCitizenshipStatus.CITIZEN, false, KrElectionType.PRESIDENTIAL_ELECTION),
                        false,
                        "voter age must be at least 18"),
                Arguments.of(
                        "citizens can vote in presidential elections",
                        request(18, KrCitizenshipStatus.CITIZEN, false, KrElectionType.PRESIDENTIAL_ELECTION),
                        true,
                        "eligible Korean citizen voter"),
                Arguments.of(
                        "citizens can vote in national assembly elections",
                        request(18, KrCitizenshipStatus.CITIZEN, false, KrElectionType.NATIONAL_ASSEMBLY_ELECTION),
                        true,
                        "eligible Korean citizen voter"),
                Arguments.of(
                        "citizens can vote in local elections",
                        request(18, KrCitizenshipStatus.CITIZEN, false, KrElectionType.LOCAL_ELECTION),
                        true,
                        "eligible Korean citizen voter"),
                Arguments.of(
                        "citizens can vote in superintendent elections",
                        request(18, KrCitizenshipStatus.CITIZEN, false, KrElectionType.SUPERINTENDENT_ELECTION),
                        true,
                        "eligible Korean citizen voter"),
                Arguments.of(
                        "citizens can vote in by-elections",
                        request(18, KrCitizenshipStatus.CITIZEN, false, KrElectionType.BY_ELECTION),
                        true,
                        "eligible Korean citizen voter"),
                Arguments.of(
                        "permanent residents can vote in local elections",
                        request(18, KrCitizenshipStatus.FOREIGN_PERMANENT_RESIDENT, true, KrElectionType.LOCAL_ELECTION),
                        true,
                        "eligible permanent resident local voter"),
                Arguments.of(
                        "permanent residents can vote in superintendent elections",
                        request(18, KrCitizenshipStatus.FOREIGN_PERMANENT_RESIDENT, true, KrElectionType.SUPERINTENDENT_ELECTION),
                        true,
                        "eligible permanent resident local voter"),
                Arguments.of(
                        "local non-citizens require permanent resident status",
                        request(18, KrCitizenshipStatus.OTHER_FOREIGNER, false, KrElectionType.LOCAL_ELECTION),
                        false,
                        "local elections require Korean citizenship or permanent resident status"),
                Arguments.of(
                        "non-citizens cannot vote in presidential or national elections",
                        request(18, KrCitizenshipStatus.FOREIGN_PERMANENT_RESIDENT, true, KrElectionType.PRESIDENTIAL_ELECTION),
                        false,
                        "national elections require Korean citizenship"));
    }

    private static KrVoterEligibilityRequest request(
            int voterAge,
            KrCitizenshipStatus citizenship,
            boolean permanentResident,
            KrElectionType electionType) {
        return new KrVoterEligibilityRequest(voterAge, citizenship, permanentResident, electionType);
    }
}
