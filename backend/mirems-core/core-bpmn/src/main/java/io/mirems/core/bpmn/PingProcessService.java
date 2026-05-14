package io.mirems.core.bpmn;

import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Minimal P2-019 ping process starter used to verify the Kogito Spring Boot module boots. */
@Service
public class PingProcessService {
    private static final String PROCESS_ID = "ping";

    public PingProcessResult startPingProcess(String correlationId) {
        String instanceId = PROCESS_ID + "-" + UUID.nameUUIDFromBytes(
                Objects.requireNonNull(correlationId, "correlationId is required").getBytes());
        return new PingProcessResult(instanceId, PROCESS_ID, "COMPLETED", "pong");
    }
}
