package io.mirems.core.api.documentation;

import io.mirems.core.bpmn.process.KogitoProcessAdapter;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("kogito")
public class KogitoProcessEngineHealthIndicator implements HealthIndicator {
    private final ObjectProvider<KogitoProcessAdapter> kogitoProcessAdapter;

    public KogitoProcessEngineHealthIndicator(ObjectProvider<KogitoProcessAdapter> kogitoProcessAdapter) {
        this.kogitoProcessAdapter = kogitoProcessAdapter;
    }

    @Override
    public Health health() {
        KogitoProcessAdapter adapter = kogitoProcessAdapter.getIfAvailable();
        if (adapter == null) {
            return Health.unknown()
                    .withDetails(Map.of(
                            "engine", "Kogito",
                            "available", false,
                            "reason", "Kogito process adapter bean is not initialized"))
                    .build();
        }
        return Health.up()
                .withDetails(Map.of(
                        "engine", "Kogito",
                        "available", true,
                        "adapter", adapter.getClass().getName()))
                .build();
    }
}
