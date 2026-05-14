package io.mirems.core.bpmn.publication;

import java.util.List;

public record ElectionPublicationResult(
        boolean passed,
        Object event,
        List<String> failureReasons) {
    public ElectionPublicationResult {
        failureReasons = List.copyOf(failureReasons == null ? List.of() : failureReasons);
    }

    public static ElectionPublicationResult passed(Object event) {
        return new ElectionPublicationResult(true, event, List.of());
    }

    public static ElectionPublicationResult failed(ElectionValidationFailedEvent event) {
        return new ElectionPublicationResult(false, event, event.reasons());
    }
}
