package io.mirems.core.infra.observability;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/** Adds stable MiREMS build and platform metadata to the Actuator info endpoint. */
@Component
public class MiremsInfoContributor implements InfoContributor {
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("mirems", MiremsInfo.builder().build());
    }

    private record MiremsInfo(String platform, String version, String vvsgAlignment) {
        private static Builder builder() {
            return new Builder();
        }

        private static final class Builder {
            private MiremsInfo build() {
                return new MiremsInfo("MiREMS Platform", "0.1.0-SNAPSHOT", "VVSG 2.0 traceability baseline");
            }
        }
    }
}
