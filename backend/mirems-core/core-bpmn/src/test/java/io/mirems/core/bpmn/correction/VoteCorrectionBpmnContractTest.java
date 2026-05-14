package io.mirems.core.bpmn.correction;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class VoteCorrectionBpmnContractTest {
    @Test
    void voteCorrectionProcessDefinesDualElectionAdminApprovalsAndAuditServiceTask() throws Exception {
        Path bpmn = Path.of("src/main/resources/processes/VoteCorrectionProcess.bpmn");
        assertThat(Files.exists(bpmn)).isTrue();

        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(bpmn.toFile());

        Element process = (Element) document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process")
                .item(0);
        assertThat(process.getAttribute("id")).isEqualTo("VoteCorrectionProcess");

        String xml = Files.readString(bpmn);
        assertThat(xml).contains("Request administrative vote correction");
        assertThat(xml).contains("First ELECTION_ADMIN approval");
        assertThat(xml).contains("Second ELECTION_ADMIN approval");
        assertThat(xml).contains("candidateUsers=firstElectionAdmin");
        assertThat(xml).contains("candidateUsers=secondElectionAdmin");
        assertThat(xml).contains("enforceDistinctApprovers");
        assertThat(xml).contains("createVoteCorrectionRecord");
        assertThat(xml).contains("emitVoteCorrectedEvent");
    }
}
