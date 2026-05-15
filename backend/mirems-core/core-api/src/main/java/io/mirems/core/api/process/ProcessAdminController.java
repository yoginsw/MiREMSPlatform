package io.mirems.core.api.process;

import io.mirems.core.bpmn.process.ProcessAuditEntry;
import io.mirems.core.bpmn.process.ProcessMonitoringService;
import io.mirems.core.bpmn.process.ProcessSignalCommand;
import io.mirems.core.bpmn.process.ProcessStatus;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping(path = "/admin/processes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProcessAdminController {
    private final ProcessMonitoringService processMonitoringService;

    public ProcessAdminController(ProcessMonitoringService processMonitoringService) {
        this.processMonitoringService = processMonitoringService;
    }

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping
    public List<ProcessStatus> listActiveProcesses() {
        return processMonitoringService.listActiveProcesses();
    }

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @PostMapping(path = "/{id}/signal", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProcessStatus signalProcess(@PathVariable("id") String id, @RequestBody ProcessSignalCommand command) {
        return processMonitoringService.signalProcess(id, command);
    }

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @GetMapping("/{id}/audit")
    public List<ProcessAuditEntry> auditTrail(@PathVariable("id") String id) {
        return processMonitoringService.getAuditTrail(id);
    }
}
