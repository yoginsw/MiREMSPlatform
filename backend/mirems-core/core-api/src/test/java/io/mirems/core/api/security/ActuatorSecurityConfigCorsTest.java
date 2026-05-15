package io.mirems.core.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class ActuatorSecurityConfigCorsTest {
    @Test
    void nonProdCorsAllowsOnlyConfiguredFrontendOrigin() {
        ApiSecurityProperties properties = new ApiSecurityProperties();
        properties.getCors().setFrontendOrigin("http://localhost:5173");
        properties.getCors().setProdAllowedOrigins(List.of("https://mirems.example"));

        CorsConfiguration configuration = configurationFor(properties, new MockEnvironment());

        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173");
    }

    @Test
    void prodCorsUsesExplicitProductionAllowList() {
        ApiSecurityProperties properties = new ApiSecurityProperties();
        properties.getCors().setFrontendOrigin("http://localhost:5173");
        properties.getCors().setProdAllowedOrigins(List.of("https://mirems.example"));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        CorsConfiguration configuration = configurationFor(properties, environment);

        assertThat(configuration.getAllowedOrigins()).containsExactly("https://mirems.example");
    }

    private CorsConfiguration configurationFor(ApiSecurityProperties properties, MockEnvironment environment) {
        ActuatorSecurityConfig config = new ActuatorSecurityConfig();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/audit");
        return config.corsConfigurationSource(properties, environment).getCorsConfiguration(request);
    }
}
