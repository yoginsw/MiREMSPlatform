package io.mirems.core.bpmn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = KogitoBpmnApplication.class)
class PingProcessIntegrationTest {
    @Autowired
    private PingProcessService pingProcessService;

    @Test
    void startsPingProcessAndCompletesWithPong() {
        PingProcessResult result = pingProcessService.startPingProcess("P2-019-red");

        assertNotNull(result.instanceId());
        assertEquals("ping", result.processId());
        assertEquals("COMPLETED", result.status());
        assertEquals("pong", result.output());
    }
}
