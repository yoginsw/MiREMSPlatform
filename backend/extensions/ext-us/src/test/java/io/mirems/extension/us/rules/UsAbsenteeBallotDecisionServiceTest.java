package io.mirems.extension.us.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

class UsAbsenteeBallotDecisionServiceTest {
    private static final Path DMN = Path.of("src/main/resources/decisions/us/UsAbsenteeBallot.dmn");
    private static final LocalDate ELECTION_DAY = LocalDate.of(2028, 11, 7);

    private final UsAbsenteeBallotDecisionService service = new UsAbsenteeBallotDecisionService();

    @ParameterizedTest(name = "{0}")
    @MethodSource("decisionRows")
    void evaluatesEveryUsAbsenteeBallotDecisionRow(
            String scenario,
            UsAbsenteeBallotRequest request,
            boolean expectedEligible,
            boolean expectedFederalWriteInAllowed,
            String expectedReason) {
        UsAbsenteeBallotResult result = service.evaluate(request);

        assertThat(result.eligible()).as(scenario).isEqualTo(expectedEligible);
        assertThat(result.federalWriteInAbsenteeBallotAllowed()).as(scenario).isEqualTo(expectedFederalWriteInAllowed);
        assertThat(result.reason()).isEqualTo(expectedReason);
    }

    @Test
    void absenteeBallotDmnDeclaresInputsOutputsAndRows() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());
        String xml = Files.readString(DMN);

        assertThat(xml).contains("name=\"voterCategory\"");
        assertThat(xml).contains("name=\"ballotRequestDate\"");
        assertThat(xml).contains("name=\"electionDay\"");
        assertThat(xml).contains("name=\"stateCode\"");
        assertThat(xml).contains("name=\"blankBallotNotReceived\"");
        assertThat(xml).contains("[0..45]");
        assertThat(xml).contains("name=\"eligible\"");
        assertThat(xml).contains("name=\"federalWriteInAbsenteeBallotAllowed\"");
        assertThat(xml).contains("name=\"reason\"");
        assertThat(xml).contains("Rule_UocavaMilitary");
        assertThat(document.getElementsByTagNameNS("*", "rule").getLength()).isEqualTo(5);
    }

    private static Stream<Arguments> decisionRows() {
        return Stream.of(
                Arguments.of(
                        "active-duty military voter is UOCAVA eligible",
                        request(UsAbsenteeVoterCategory.MILITARY, ELECTION_DAY.minusDays(45), false),
                        true,
                        false,
                        "eligible UOCAVA military voter"),
                Arguments.of(
                        "overseas citizen voter is UOCAVA eligible",
                        request(UsAbsenteeVoterCategory.OVERSEAS_CITIZEN, ELECTION_DAY.minusDays(45), false),
                        true,
                        false,
                        "eligible UOCAVA overseas citizen voter"),
                Arguments.of(
                        "late UOCAVA request allows federal write-in absentee ballot fallback",
                        request(UsAbsenteeVoterCategory.MILITARY, ELECTION_DAY.minusDays(20), true),
                        true,
                        true,
                        "UOCAVA late request permits federal write-in absentee ballot"),
                Arguments.of(
                        "post-election UOCAVA request does not qualify for FWAB fallback",
                        request(UsAbsenteeVoterCategory.MILITARY, ELECTION_DAY.plusDays(1), true),
                        true,
                        false,
                        "eligible UOCAVA military voter"),
                Arguments.of(
                        "domestic no-excuse absentee voters are governed by state law snapshot",
                        request(UsAbsenteeVoterCategory.DOMESTIC_NO_EXCUSE, ELECTION_DAY.minusDays(20), false),
                        true,
                        false,
                        "eligible state absentee voter"),
                Arguments.of(
                        "unknown category is not absentee eligible",
                        request(UsAbsenteeVoterCategory.NOT_ABSENTEE_ELIGIBLE, ELECTION_DAY.minusDays(20), false),
                        false,
                        false,
                        "absentee category is not eligible"));
    }

    private static UsAbsenteeBallotRequest request(
            UsAbsenteeVoterCategory category, LocalDate requestDate, boolean blankBallotNotReceived) {
        return new UsAbsenteeBallotRequest(category, requestDate, ELECTION_DAY, "CA", blankBallotNotReceived);
    }
}
