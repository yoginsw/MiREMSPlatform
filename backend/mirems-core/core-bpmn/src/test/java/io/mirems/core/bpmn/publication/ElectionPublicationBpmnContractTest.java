package io.mirems.core.bpmn.publication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.kie.kogito.internal.process.workitem.KogitoWorkItem;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemManager;
import org.kie.kogito.internal.process.workitem.WorkItemTransition;
import org.w3c.dom.Document;

class ElectionPublicationBpmnContractTest {
    private static final Path PROCESS = Path.of("src/main/resources/processes/ElectionPublicationProcess.bpmn");

    @Test
    void bpmnDefinesElectionPublicationProcessWithRequiredReviewValidationAndPublicationFlow() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(PROCESS.toFile());

        assertThat(document.getElementsByTagNameNS("*", "process").item(0).getAttributes()
                        .getNamedItem("id").getNodeValue())
                .isEqualTo("election-publication");
        assertThat(document.getElementsByTagNameNS("*", "process").item(0).getAttributes()
                        .getNamedItem("isExecutable").getNodeValue())
                .isEqualTo("true");
        assertThat(Files.readString(PROCESS))
                .contains("ELECTION_ADMIN")
                .contains("Validate required contests")
                .contains("Validate ballot style coverage")
                .contains("Validation passed?")
                .contains("Publish election")
                .contains("ElectionValidationFailedEvent");
    }

    @Test
    void serviceTaskBeansAreKogitoWorkItemHandlers() {
        assertThat(List.of(
                        new RequiredContestsValidationWorkItemHandler(),
                        new BallotStyleCoverageValidationWorkItemHandler(),
                        new PublishElectionWorkItemHandler()))
                .allSatisfy(handler -> assertThat(handler).isInstanceOf(KogitoWorkItemHandler.class));
    }

    @Test
    void abstractWorkItemHandlerProvidesSafeKogitoDefaults() {
        RequiredContestsValidationWorkItemHandler handler = new RequiredContestsValidationWorkItemHandler();
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
