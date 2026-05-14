package io.mirems.core.bpmn.publication;

import io.mirems.core.domain.election.event.ElectionPublishedEvent;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PublishElectionWorkItemHandler extends AbstractElectionPublicationWorkItemHandler {
    public ElectionPublicationResult publish(ElectionPublicationContext context) {
        if (!context.passed()) {
            return ElectionPublicationResult.failed(new ElectionValidationFailedEvent(
                    context.request().election().getId(),
                    context.failureReasons(),
                    java.time.OffsetDateTime.now()));
        }

        context.request().election().publish();
        ElectionPublishedEvent publishedEvent = context.request().election().pullDomainEvents().stream()
                .filter(ElectionPublishedEvent.class::isInstance)
                .map(ElectionPublishedEvent.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("ElectionPublishedEvent was not emitted"));
        return new ElectionPublicationResult(true, publishedEvent, List.of());
    }
}
