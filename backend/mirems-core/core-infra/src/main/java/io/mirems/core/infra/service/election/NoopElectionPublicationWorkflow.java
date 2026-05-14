package io.mirems.core.infra.service.election;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** No-op implementation until the P2 BPMN process adapter is available. */
@Component
public class NoopElectionPublicationWorkflow implements ElectionPublicationWorkflow {
    @Override
    public void publish(UUID electionId) {
        // P1-016 requires a BPMN delegation point; P2 will replace this no-op adapter.
    }
}
