package io.mirems.core.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class MiremsSecurityContextTest {
    private final MiremsSecurityContext securityContext = new MiremsSecurityContext();

    @Test
    void extractsUserRolesAndElectionScopeFromJwtClaims() {
        Jwt jwt = jwt(Map.of(
                "sub", "user-123",
                "preferred_username", "election-admin",
                "realm_access", Map.of("roles", List.of("ELECTION_ADMIN", "OBSERVER")),
                "mirems_election_scope", List.of("election-a", "election-b")));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        try {
            assertThat(securityContext.userId()).isEqualTo("user-123");
            assertThat(securityContext.roles()).containsExactlyInAnyOrder("ELECTION_ADMIN", "OBSERVER");
            assertThat(securityContext.electionScope()).containsExactly("election-a", "election-b");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void fallsBackToGrantedAuthoritiesWhenAuthenticationIsNotJwt() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "local-user",
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_AUDITOR"))));

        try {
            assertThat(securityContext.userId()).isEqualTo("local-user");
            assertThat(securityContext.roles()).containsExactly("AUDITOR");
            assertThat(securityContext.electionScope()).isEmpty();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Jwt jwt(Map<String, Object> claims) {
        Instant now = Instant.now();
        return new Jwt("token", now, now.plusSeconds(300), Map.of("alg", "none"), claims);
    }
}
