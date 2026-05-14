package io.mirems.core.bpmn.tabulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class BallotTabulationBpmnContractTest {
    private static final Path PROCESS = Path.of("src/main/resources/processes/BallotTabulationProcess.bpmn");

    @Test
    void bpmnDefinesBallotTabulationProcessWithRequiredLoadAggregateReviewLockAndPublishFlow() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(PROCESS.toFile());

        assertThat(document.getElementsByTagNameNS("*", "process").item(0).getAttributes()
                        .getNamedItem("id").getNodeValue())
                .isEqualTo("ballot-tabulation");
        assertThat(document.getElementsByTagNameNS("*", "process").item(0).getAttributes()
                        .getNamedItem("isExecutable").getNodeValue())
                .isEqualTo("true");
        assertThat(Files.readString(PROCESS))
                .contains("ElectionStatus changed to CLOSED")
                .contains("Load all VotingResult records for election")
                .contains("Aggregate by Contest and create TabulationReport draft")
                .contains("TABULATION_OFFICER")
                .contains("Lock report, generate SHA-256 hash, emit TabulationCompletedEvent")
                .contains("Public results enabled?")
                .contains("Publish results")
                .contains("Tabulation completed");
    }
}
