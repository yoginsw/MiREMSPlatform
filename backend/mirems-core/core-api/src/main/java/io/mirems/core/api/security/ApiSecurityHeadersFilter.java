package io.mirems.core.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiSecurityHeadersFilter extends OncePerRequestFilter {
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'";
    static final String STRICT_TRANSPORT_SECURITY = "max-age=31536000; includeSubDomains";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Strict-Transport-Security", STRICT_TRANSPORT_SECURITY);
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        filterChain.doFilter(request, response);
    }
}
