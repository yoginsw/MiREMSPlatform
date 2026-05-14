package io.mirems.core.bpmn.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.kie.kogito.internal.process.workitem.KogitoWorkItem;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemManager;
import org.kie.kogito.internal.process.workitem.WorkItemTransition;
import org.w3c.dom.Document;

class CandidateRegistrationBpmnContractTest {
    private static final Path PROCESS = Path.of("src/main/resources/processes/CandidateRegistrationProcess.bpmn");
    private static final Path DMN = Path.of("src/main/resources/decisions/CandidateEligibilityCheck.dmn");

    @Test
    void bpmnDefinesCandidateRegistrationProcessWithEligibilityReviewDecisionAndTimeout() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(PROCESS.toFile());

        assertThat(document.getElementsByTagNameNS("*", "process").item(0).getAttributes()
                        .getNamedItem("id").getNodeValue())
                .isEqualTo("candidate-registration");
        assertThat(document.getElementsByTagNameNS("*", "process").item(0).getAttributes()
                        .getNamedItem("isExecutable").getNodeValue())
                .isEqualTo("true");
        assertThat(Files.readString(PROCESS))
                .contains("Candidate submits registration")
                .contains("Validate candidate eligibility")
                .contains("CandidateEligibilityCheck.dmn")
                .contains("ELECTION_OFFICER")
                .contains("Approved?")
                .contains("Approve candidate")
                .contains("Disqualify candidate")
                .contains("PT72H")
                .contains("Send rejection notification");
    }

    @Test
    void dmnDefinesAgeAndResidencyEligibilityRules() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(DMN.toFile());

        assertThat(document.getDocumentElement().getAttributes().getNamedItem("name").getNodeValue())
                .isEqualTo("CandidateEligibilityCheck");
        assertThat(Files.readString(DMN))
                .contains("candidateAge")
                .contains("residencyVerified")
                .contains("eligible")
                .contains("age must be at least 35")
                .contains("residency must be verified");
    }

    @Test
    void serviceTaskBeansAreKogitoWorkItemHandlers() {
        assertThat(List.of(
                        new CandidateEligibilityCheckHandler(),
                        new CandidateApprovalWorkItemHandler(),
                        new CandidateRejectionNotificationWorkItemHandler()))
                .allSatisfy(handler -> assertThat(handler).isInstanceOf(KogitoWorkItemHandler.class));
    }

    @Test
    void abstractWorkItemHandlerProvidesSafeKogitoDefaults() {
        CandidateEligibilityCheckHandler handler = new CandidateEligibilityCheckHandler();
        WorkItemTransition transition = mock(WorkItemTransition.class);

        assertThat(handler.getApplication()).isNull();
        assertThat(handler.allowedTransitions("active")).isEmpty();
        assertThat(handler.transitionToPhase(
                        mock(KogitoWorkItemManager.class),
                        mock(KogitoWorkItem.class),
                        transition))
                .containsSame(transition);
        assertThat(handler.newTransition("complete", "Completed", Map.of())).isNull();
        assertThat(handler.startingTransition(Map.of())).isNull();
        assertThat(handler.completeTransition("Completed", Map.of())).isNull();
        assertThat(handler.abortTransition("Aborted")).isNull();
    }
}
