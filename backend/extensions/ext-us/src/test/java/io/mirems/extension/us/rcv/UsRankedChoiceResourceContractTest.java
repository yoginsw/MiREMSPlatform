package io.mirems.extension.us.rcv;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class UsRankedChoiceResourceContractTest {
    private static final Path DMN = Path.of("src/main/resources/decisions/us/UsInstantRunoffTabulation.dmn");
    private static final Path BPMN = Path.of("src/main/resources/processes/us/UsRankedChoiceTabulationProcess.bpmn");

    @Test
    void instantRunoffDmnDeclaresExecutableAlgorithmInputsOutputsAndTieBreaks() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());
        String xml = Files.readString(DMN);

        assertThat(document.getElementsByTagNameNS("*", "decision").item(0).getAttributes()
                        .getNamedItem("name").getNodeValue())
                .isEqualTo("UsInstantRunoffTabulation");
        assertThat(xml).contains("name=\"contestId\"");
        assertThat(xml).contains("name=\"candidateIds\"");
        assertThat(xml).contains("name=\"rankedBallots\"");
        assertThat(xml).contains("name=\"winnerCandidateId\"");
        assertThat(xml).contains("name=\"rounds\"");
        assertThat(xml).contains("rankValidation: every ballot in rankedBallots satisfies unique ranks and candidateIds containment");
        assertThat(xml).contains("majorityThreshold: floor(activeBallotCount / 2) + 1");
        assertThat(xml).contains("eliminatedCandidateId: max(lowestTallyCandidateIds) /* descending UUID tie-break */");
        assertThat(xml).contains("transferRule: next ranked candidate where candidate in activeCandidateIds");
        assertThat(xml).contains("javaExecutableService: io.mirems.extension.us.rcv.UsInstantRunoffTabulationService");
        assertThat(xml).doesNotContain("valid input").doesNotContain("instant-runoff placeholder");
    }

    @Test
    void rankedChoiceBpmnDefinesLoadValidateDmnTabulateReviewAndPublishFlow() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(BPMN.toFile());
        String xml = Files.readString(BPMN);

        assertThat(document.getElementsByTagNameNS("*", "process").item(0).getAttributes()
                        .getNamedItem("id").getNodeValue())
                .isEqualTo("us-ranked-choice-tabulation");
        assertThat(xml)
                .contains("RANKED_CHOICE contest closed")
                .contains("Load ranked choice ballots")
                .contains("Validate one-to-N unique candidate ranks")
                .contains("Run UsInstantRunoffTabulation DMN")
                .contains("TABULATION_OFFICER reviews RCV rounds")
                .contains("Publish RCV winner and round audit summary")
                .contains("RCV tabulation completed");
    }
}
