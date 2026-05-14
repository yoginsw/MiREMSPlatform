package io.mirems.core.infra.service.election;

import java.util.UUID;

/** Stub boundary for the future BPMN-backed election publication workflow. */
public interface ElectionPublicationWorkflow {
    void publish(UUID electionId);
}
