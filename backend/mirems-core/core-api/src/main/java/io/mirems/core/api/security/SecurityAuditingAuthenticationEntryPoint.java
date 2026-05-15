package io.mirems.core.api.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditingAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final String BASIC_AUTH_SCHEME = "basic ";

    private final SecurityAuditEventRecorder recorder;
    private final AuthenticationEntryPoint basicDelegate;
    private final AuthenticationEntryPoint bearerDelegate = new BearerTokenAuthenticationEntryPoint();

    public SecurityAuditingAuthenticationEntryPoint(SecurityAuditEventRecorder recorder) {
        this.recorder = recorder;
        BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName("MiREMS");
        this.basicDelegate = basicAuthenticationEntryPoint;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        recorder.recordAuthenticationFailure(request, authException);
        delegateFor(request).commence(request, response, authException);
    }

    private AuthenticationEntryPoint delegateFor(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.toLowerCase(Locale.ROOT).startsWith(BASIC_AUTH_SCHEME)) {
            return basicDelegate;
        }
        return bearerDelegate;
    }
}
