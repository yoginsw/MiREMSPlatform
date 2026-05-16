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

class KrCandidateEligibilityDecisionServiceTest {
    private static final Path DMN = Path.of("src/main/resources/decisions/kr/KrCandidateEligibility.dmn");

    private final KrCandidateEligibilityDecisionService service = new KrCandidateEligibilityDecisionService();

    @ParameterizedTest(name = "{0}")
    @MethodSource("decisionRows")
    void evaluatesEveryKrCandidateEligibilityDecisionRow(
            String scenario,
            KrCandidateEligibilityRequest request,
            boolean expectedEligible,
            String expectedReason) {
        KrCandidateEligibilityResult result = service.evaluate(request);

        assertThat(result.eligible()).as(scenario).isEqualTo(expectedEligible);
        assertThat(result.reason()).isEqualTo(expectedReason);
    }

    @Test
    void candidateEligibilityDmnDeclaresInputsOutputsAndDecisionRows() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());
        String xml = Files.readString(DMN);

        assertThat(xml).contains("name=\"candidateAge\"");
        assertThat(xml).contains("name=\"citizenship\"");
        assertThat(xml).contains("name=\"hasDisqualifyingCriminalRecord\"");
        assertThat(xml).contains("name=\"electionType\"");
        assertThat(xml).contains("name=\"eligible\"");
        assertThat(xml).contains("name=\"reason\"");
        assertThat(document.getElementsByTagNameNS("*", "rule").getLength()).isEqualTo(5);
    }

    private static Stream<Arguments> decisionRows() {
        return Stream.of(
                Arguments.of(
                        "candidates must be Korean citizens",
                        request(45, KrCitizenshipStatus.FOREIGN_PERMANENT_RESIDENT, false, KrElectionType.PRESIDENTIAL_ELECTION),
                        false,
                        "candidate must be a Korean citizen"),
                Arguments.of(
                        "criminal disqualification blocks candidacy",
                        request(45, KrCitizenshipStatus.CITIZEN, true, KrElectionType.NATIONAL_ASSEMBLY_ELECTION),
                        false,
                        "candidate has a disqualifying criminal record"),
                Arguments.of(
                        "presidential candidates must be at least 40",
                        request(39, KrCitizenshipStatus.CITIZEN, false, KrElectionType.PRESIDENTIAL_ELECTION),
                        false,
                        "presidential candidates must be at least 40"),
                Arguments.of(
                        "non-presidential public election candidates must be at least 18",
                        request(17, KrCitizenshipStatus.CITIZEN, false, KrElectionType.LOCAL_ELECTION),
                        false,
                        "non-presidential candidates must be at least 18"),
                Arguments.of(
                        "qualified citizens are eligible candidates",
                        request(18, KrCitizenshipStatus.CITIZEN, false, KrElectionType.NATIONAL_ASSEMBLY_ELECTION),
                        true,
                        "eligible Korean citizen candidate"));
    }

    private static KrCandidateEligibilityRequest request(
            int candidateAge,
            KrCitizenshipStatus citizenship,
            boolean hasDisqualifyingCriminalRecord,
            KrElectionType electionType) {
        return new KrCandidateEligibilityRequest(
                candidateAge, citizenship, hasDisqualifyingCriminalRecord, electionType);
    }
}
