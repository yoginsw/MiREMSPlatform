package io.mirems.core.bpmn.voter;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.domain.election.ElectionType;
import io.mirems.core.domain.voting.RegistrationStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

class VoterEligibilityDecisionServiceTest {
    private static final Path DMN = Path.of("src/main/resources/decisions/VoterEligibilityCheck.dmn");

    private final VoterEligibilityDecisionService service = new VoterEligibilityDecisionService();

    @ParameterizedTest(name = "{0}")
    @MethodSource("decisionRows")
    void evaluatesEveryVoterEligibilityDecisionRow(
            String scenario,
            VoterEligibilityRequest request,
            boolean expectedEligible,
            String expectedReason) {
        VoterEligibilityResult result = service.evaluate(request);

        assertThat(result.eligible()).as(scenario).isEqualTo(expectedEligible);
        assertThat(result.reason()).isEqualTo(expectedReason);
    }

    @Test
    void voterEligibilityDmnDeclaresAllInputsOutputsAndDecisionRows() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());
        String xml = Files.readString(DMN);

        assertThat(xml).contains("name=\"voterAge\"");
        assertThat(xml).contains("name=\"registrationStatus\"");
        assertThat(xml).contains("name=\"residencyVerified\"");
        assertThat(xml).contains("name=\"electionType\"");
        assertThat(xml).contains("name=\"eligible\"");
        assertThat(xml).contains("name=\"reason\"");
        assertThat(document.getElementsByTagNameNS("*", "rule").getLength()).isEqualTo(6);
    }

    private static Stream<Arguments> decisionRows() {
        return Stream.of(
                Arguments.of(
                        "under 18 voters are ineligible for every election type",
                        request(17, RegistrationStatus.ACTIVE, true, ElectionType.PRESIDENTIAL),
                        false,
                        "voter age must be at least 18"),
                Arguments.of(
                        "non-active registration is ineligible",
                        request(18, RegistrationStatus.SUSPENDED, true, ElectionType.PRESIDENTIAL),
                        false,
                        "voter registration status must be ACTIVE"),
                Arguments.of(
                        "unverified residency is ineligible",
                        request(18, RegistrationStatus.ACTIVE, false, ElectionType.PRESIDENTIAL),
                        false,
                        "voter residency must be verified"),
                Arguments.of(
                        "presidential elections require minimum age 18",
                        request(18, RegistrationStatus.ACTIVE, true, ElectionType.PRESIDENTIAL),
                        true,
                        "eligible"),
                Arguments.of(
                        "referendums require minimum age 18",
                        request(18, RegistrationStatus.ACTIVE, true, ElectionType.REFERENDUM),
                        true,
                        "eligible"),
                Arguments.of(
                        "local elections require minimum age 19 by core policy until extension packs override it",
                        request(18, RegistrationStatus.ACTIVE, true, ElectionType.LOCAL),
                        false,
                        "LOCAL elections require voter age at least 19 by core policy"));
    }

    private static VoterEligibilityRequest request(
            int age,
            RegistrationStatus registrationStatus,
            boolean residencyVerified,
            ElectionType electionType) {
        return new VoterEligibilityRequest(age, registrationStatus, residencyVerified, electionType);
    }
}
