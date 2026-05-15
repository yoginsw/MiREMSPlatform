package io.mirems.core.api.security;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mirems.security")
public class ApiSecurityProperties {
    private final RateLimit rateLimit = new RateLimit();
    private final Cors cors = new Cors();

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Cors getCors() {
        return cors;
    }

    public static final class RateLimit {
        private boolean enabled = true;
        private long capacity = 60;
        private long refillTokens = 60;
        private Duration refillPeriod = Duration.ofMinutes(1);
        private List<String> sensitiveEndpointPatterns = List.of(
                "/sessions",
                "/sessions/*/cast",
                "/sessions/*/spoil",
                "/elections/*/tabulate",
                "/audit");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(long refillTokens) {
            this.refillTokens = refillTokens;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(Duration refillPeriod) {
            this.refillPeriod = refillPeriod;
        }

        public List<String> getSensitiveEndpointPatterns() {
            return sensitiveEndpointPatterns;
        }

        public void setSensitiveEndpointPatterns(List<String> sensitiveEndpointPatterns) {
            this.sensitiveEndpointPatterns = List.copyOf(sensitiveEndpointPatterns);
        }
    }

    public static final class Cors {
        private String frontendOrigin = "http://localhost:5173";
        private List<String> prodAllowedOrigins = List.of();

        public String getFrontendOrigin() {
            return frontendOrigin;
        }

        public void setFrontendOrigin(String frontendOrigin) {
            this.frontendOrigin = frontendOrigin;
        }

        public List<String> getProdAllowedOrigins() {
            return prodAllowedOrigins;
        }

        public void setProdAllowedOrigins(List<String> prodAllowedOrigins) {
            this.prodAllowedOrigins = List.copyOf(prodAllowedOrigins);
        }
    }
}
