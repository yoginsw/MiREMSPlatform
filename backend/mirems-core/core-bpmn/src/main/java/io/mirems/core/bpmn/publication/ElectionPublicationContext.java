package io.mirems.core.bpmn.publication;

import java.util.ArrayList;
import java.util.List;

public record ElectionPublicationContext(
        ElectionPublicationRequest request,
        List<String> failureReasons) {
    public ElectionPublicationContext {
        failureReasons = new ArrayList<>(failureReasons == null ? List.of() : failureReasons);
    }

    public static ElectionPublicationContext from(ElectionPublicationRequest request) {
        return new ElectionPublicationContext(request, new ArrayList<>());
    }

    public void fail(String reason) {
        failureReasons.add(reason);
    }

    public boolean passed() {
        return failureReasons.isEmpty();
    }
}
