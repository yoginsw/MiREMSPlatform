package io.mirems.core.api.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiRateLimitingFilter extends OncePerRequestFilter {
    private final ApiSecurityProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    public ApiRateLimitingFilter(ApiSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.getRateLimit().isEnabled() || !isSensitiveEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        ConsumptionProbe ipProbe = bucket(ipBuckets, "ip:" + sourceIp(request)).tryConsumeAndReturnRemaining(1);
        if (!ipProbe.isConsumed()) {
            reject(response, ipProbe);
            return;
        }

        String userKey = userKey(request);
        if (userKey != null) {
            ConsumptionProbe userProbe = bucket(userBuckets, "user:" + userKey).tryConsumeAndReturnRemaining(1);
            if (!userProbe.isConsumed()) {
                reject(response, userProbe);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSensitiveEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return properties.getRateLimit().getSensitiveEndpointPatterns().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Bucket bucket(Map<String, Bucket> buckets, String key) {
        return buckets.computeIfAbsent(key, ignored -> Bucket.builder().addLimit(limit()).build());
    }

    private Bandwidth limit() {
        ApiSecurityProperties.RateLimit rateLimit = properties.getRateLimit();
        return Bandwidth.builder()
                .capacity(rateLimit.getCapacity())
                .refillIntervally(rateLimit.getRefillTokens(), rateLimit.getRefillPeriod())
                .build();
    }

    private String userKey(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal == null) {
            return null;
        }
        if (principal instanceof Authentication authentication) {
            return authentication.getName();
        }
        return principal.getName();
    }

    private String sourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"Too many requests","status":429,"detail":"API rate limit exceeded"}
                """);
    }
}
