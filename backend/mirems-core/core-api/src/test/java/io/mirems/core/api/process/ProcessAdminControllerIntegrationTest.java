package io.mirems.core.api.process;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.mirems.core.bpmn.process.ProcessAuditEntry;
import io.mirems.core.bpmn.process.ProcessMonitoringService;
import io.mirems.core.bpmn.process.ProcessSignalCommand;
import io.mirems.core.bpmn.process.ProcessStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
class ProcessAdminControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessMonitoringService processMonitoringService;

    @Test
    void systemAdminCanListActiveProcessInstances() throws Exception {
        when(processMonitoringService.listActiveProcesses()).thenReturn(List.of(
                new ProcessStatus("pi-001", "ElectionPublicationProcess", "ACTIVE", Map.of("electionId", "e-001"), List.of("review")),
                new ProcessStatus("pi-002", "AuditReviewProcess", "PENDING", Map.of("electionId", "e-002"), List.of())));

        mockMvc.perform(get("/admin/processes").with(user("sysadmin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instanceId").value("pi-001"))
                .andExpect(jsonPath("$[0].processId").value("ElectionPublicationProcess"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].variables.electionId").value("e-001"))
                .andExpect(jsonPath("$[0].activeNodes[0]").value("review"))
                .andExpect(jsonPath("$[1].instanceId").value("pi-002"));
    }

    @Test
    void systemAdminCanSignalProcessInstance() throws Exception {
        when(processMonitoringService.signalProcess(eq("pi-001"), eq(new ProcessSignalCommand("approve", Map.of("approved", true)))))
                .thenReturn(new ProcessStatus("pi-001", "ElectionPublicationProcess", "COMPLETED", Map.of("approved", true), List.of()));

        mockMvc.perform(post("/admin/processes/pi-001/signal")
                        .with(csrf())
                        .with(user("sysadmin").roles("SYSTEM_ADMIN"))
                        .contentType("application/json")
                        .content("{\"signalName\":\"approve\",\"payload\":{\"approved\":true}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceId").value("pi-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.variables.approved").value(true));
    }

    @Test
    void systemAdminCanGetProcessAuditTrail() throws Exception {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-06-05T10:15:30Z");
        when(processMonitoringService.getAuditTrail("pi-001")).thenReturn(List.of(
                new ProcessAuditEntry("pi-001", "ElectionPublicationProcess", "PROCESS_STARTED", "admin-1", occurredAt, Map.of("status", "ACTIVE"))));

        mockMvc.perform(get("/admin/processes/pi-001/audit").with(user("sysadmin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instanceId").value("pi-001"))
                .andExpect(jsonPath("$[0].processId").value("ElectionPublicationProcess"))
                .andExpect(jsonPath("$[0].eventType").value("PROCESS_STARTED"))
                .andExpect(jsonPath("$[0].actorId").value("admin-1"))
                .andExpect(jsonPath("$[0].payload.status").value("ACTIVE"));
    }

    @Test
    void adminEndpointsRequireSystemAdminRole() throws Exception {
        mockMvc.perform(get("/admin/processes"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/processes").with(user("election-admin").roles("ELECTION_ADMIN")))
                .andExpect(status().isForbidden());
    }
}
