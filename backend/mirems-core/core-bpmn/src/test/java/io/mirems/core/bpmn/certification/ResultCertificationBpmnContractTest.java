package io.mirems.core.bpmn.certification;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class ResultCertificationBpmnContractTest {
    @Test
    void resultCertificationProcessDefinesAdminLegalCertificationAndPdfTasks() throws Exception {
        Path bpmn = Path.of("src/main/resources/processes/ResultCertificationProcess.bpmn");
        assertThat(Files.exists(bpmn)).isTrue();

        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(bpmn.toFile());

        Element process = (Element) document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process")
                .item(0);
        assertThat(process.getAttribute("id")).isEqualTo("ResultCertificationProcess");

        String xml = Files.readString(bpmn);
        assertThat(xml).contains("Completed TabulationReport input");
        assertThat(xml).contains("ELECTION_ADMIN reviews final tabulation");
        assertThat(xml).contains("Legal/compliance review");
        assertThat(xml).contains("setElectionCertifiedAndEmitEvent");
        assertThat(xml).contains("generateOfficialPdfCertificationReport");
    }
}
