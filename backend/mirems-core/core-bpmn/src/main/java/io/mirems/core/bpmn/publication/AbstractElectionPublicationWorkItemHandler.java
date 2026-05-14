package io.mirems.core.bpmn.publication;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.kie.kogito.Application;
import org.kie.kogito.internal.process.workitem.KogitoWorkItem;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemManager;
import org.kie.kogito.internal.process.workitem.Policy;
import org.kie.kogito.internal.process.workitem.WorkItemTransition;

abstract class AbstractElectionPublicationWorkItemHandler implements KogitoWorkItemHandler {
    private Application application;

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public void setApplication(Application application) {
        this.application = application;
    }

    @Override
    public Optional<WorkItemTransition> transitionToPhase(
            KogitoWorkItemManager manager,
            KogitoWorkItem workItem,
            WorkItemTransition transition) {
        return Optional.ofNullable(transition);
    }

    @Override
    public Set<String> allowedTransitions(String phaseId) {
        return Set.of();
    }

    @Override
    public WorkItemTransition newTransition(
            String phaseId,
            String status,
            Map<String, Object> data,
            Policy... policies) {
        return null;
    }

    @Override
    public WorkItemTransition startingTransition(Map<String, Object> data, Policy... policies) {
        return null;
    }

    @Override
    public WorkItemTransition completeTransition(String phaseStatus, Map<String, Object> data, Policy... policies) {
        return null;
    }

    @Override
    public WorkItemTransition abortTransition(String phaseStatus, Policy... policies) {
        return null;
    }
}
