package io.mirems.core.bpmn.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class AuditReviewBpmnContractTest {
    @Test
    void auditReviewProcessDefinesAuditorInitiatedReviewAndReportGeneration() throws Exception {
        Path bpmn = Path.of("src/main/resources/processes/AuditReviewProcess.bpmn");
        assertThat(Files.exists(bpmn)).isTrue();

        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(bpmn.toFile());

        Element process = (Element) document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "process")
                .item(0);
        assertThat(process.getAttribute("id")).isEqualTo("AuditReviewProcess");

        String xml = Files.readString(bpmn);
        assertThat(xml).contains("AUDITOR initiates post-election audit review");
        assertThat(xml).contains("read-only access to all election data");
        assertThat(xml).contains("collectAuditEventRecords");
        assertThat(xml).contains("generateAuditReportJsonAndPdf");
        assertThat(xml).contains("AuditReport produced");
    }
}
