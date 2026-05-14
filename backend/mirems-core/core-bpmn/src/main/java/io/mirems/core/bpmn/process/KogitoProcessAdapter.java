package io.mirems.core.bpmn.process;

import java.util.List;
import java.util.Map;
import org.kie.kogito.MapOutput;
import org.kie.kogito.Model;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceReadMode;
import org.kie.kogito.process.Processes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean({Processes.class, org.kie.kogito.process.ProcessService.class})
public class KogitoProcessAdapter implements MiremsProcessService<Map<String, Object>, ProcessStatus> {
    private final Processes processes;
    private final org.kie.kogito.process.ProcessService processService;

    public KogitoProcessAdapter(Processes processes, org.kie.kogito.process.ProcessService processService) {
        this.processes = processes;
        this.processService = processService;
    }

    @Override
    public ProcessStatus startProcess(String processId, Map<String, Object> input, String correlationId) {
        org.kie.kogito.process.Process<Model> process = processById(processId);
        Model model = process.createModel();
        model.update(input == null ? Map.of() : input);
        ProcessInstance<Model> instance = processService.createProcessInstance(process, correlationId, model, null);
        instance.start();
        return toStatus(process, instance);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ProcessStatus signalProcess(String instanceId, String signalName, Object payload) {
        org.kie.kogito.process.Process process = processByInstanceId(instanceId);
        java.util.Optional<?> signalResult = processService.signalProcessInstance(process, instanceId, payload, signalName);
        if (signalResult.isEmpty()) {
            throw new KogitoProcessException("Kogito process instance not found for signal: " + instanceId);
        }
        return getStatus(instanceId);
    }

    @Override
    public ProcessStatus getStatus(String instanceId) {
        org.kie.kogito.process.Process<Model> process = processByInstanceId(instanceId);
        ProcessInstance<Model> instance = process.instances()
                .findById(instanceId, ProcessInstanceReadMode.READ_ONLY)
                .orElseThrow(() -> new KogitoProcessException("Kogito process instance not found: " + instanceId));
        return toStatus(process, instance);
    }

    @SuppressWarnings("unchecked")
    private org.kie.kogito.process.Process<Model> processById(String processId) {
        org.kie.kogito.process.Process<? extends Model> process = processes.processById(processId);
        if (process == null) {
            throw new KogitoProcessException("Kogito process not found: " + processId);
        }
        return (org.kie.kogito.process.Process<Model>) process;
    }

    @SuppressWarnings("unchecked")
    private org.kie.kogito.process.Process<Model> processByInstanceId(String instanceId) {
        return (org.kie.kogito.process.Process<Model>) processes.processByProcessInstanceId(instanceId)
                .orElseThrow(() -> new KogitoProcessException(
                        "Kogito process not found for instance: " + instanceId));
    }

    private ProcessStatus toStatus(
            org.kie.kogito.process.Process<Model> process,
            ProcessInstance<Model> instance) {
        return new ProcessStatus(
                instance.id(),
                process.id(),
                statusName(instance.status()),
                variables(instance.variables()),
                List.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> variables(Object variables) {
        if (variables instanceof MapOutput output) {
            return output.toMap();
        }
        if (variables instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String statusName(int status) {
        return switch (status) {
            case ProcessInstance.STATE_PENDING -> "PENDING";
            case ProcessInstance.STATE_ACTIVE -> "ACTIVE";
            case ProcessInstance.STATE_COMPLETED -> "COMPLETED";
            case ProcessInstance.STATE_ABORTED -> "ABORTED";
            case ProcessInstance.STATE_SUSPENDED -> "SUSPENDED";
            case ProcessInstance.STATE_ERROR -> "ERROR";
            default -> "UNKNOWN";
        };
    }
}
