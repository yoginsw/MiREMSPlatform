package io.mirems.core.api.security;

import static org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.to;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class ActuatorSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**")
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("SYSTEM_ADMIN"))
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(to(HealthEndpoint.class, MetricsEndpoint.class, InfoEndpoint.class))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(to(HealthEndpoint.class)).permitAll()
                        .requestMatchers(to(MetricsEndpoint.class, InfoEndpoint.class)).hasRole("SYSTEM_ADMIN")
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http, ApiRateLimitingFilter apiRateLimitingFilter) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/elections").hasRole("ELECTION_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/elections/*/contests").hasRole("ELECTION_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/elections/*/contests/*").hasRole("ELECTION_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/elections/*/ballots", "/elections/*/ballots/*/versions")
                        .hasRole("ELECTION_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/elections/*/ballot-styles")
                        .hasRole("ELECTION_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/voters")
                        .hasRole("ELECTION_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/sessions", "/sessions/*/cast", "/sessions/*/spoil")
                        .hasAnyRole("VOTER", "ELECTION_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/elections/*/tabulate")
                        .hasRole("TABULATION_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/audit")
                        .hasAnyRole("AUDITOR", "SYSTEM_ADMIN")
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/elections/*/ballot-styles/*")
                        .hasRole("ELECTION_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/elections/*/ballot-styles/*")
                        .hasRole("ELECTION_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/elections/*/contests/*/candidates")
                        .hasRole("ELECTION_OFFICER")
                        .requestMatchers(HttpMethod.PUT, "/elections/*/contests/*/candidates/*/withdraw")
                        .hasRole("ELECTION_OFFICER")
                        .requestMatchers(HttpMethod.PUT, "/elections/*/publish", "/elections/*/close")
                        .hasRole("ELECTION_ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/elections",
                                "/elections/*",
                                "/elections/*/contests",
                                "/elections/*/contests/*",
                                "/elections/*/contests/*/candidates",
                                "/elections/*/contests/*/candidates/*",
                                "/elections/*/ballots",
                                "/elections/*/ballots/*/preview",
                                "/elections/*/ballot-styles",
                                "/voters/*",
                                "/voters/*/eligibility/*")
                        .authenticated()
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults())
                .addFilterAfter(apiRateLimitingFilter, BasicAuthenticationFilter.class)
                .build();
    }

    @Bean
    FilterRegistrationBean<ApiRateLimitingFilter> apiRateLimitingFilterRegistration(ApiRateLimitingFilter filter) {
        FilterRegistrationBean<ApiRateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(ApiSecurityProperties properties, Environment environment) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins(properties, environment));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Forwarded-For"));
        configuration.setExposedHeaders(List.of("Location", "Retry-After"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> allowedOrigins(ApiSecurityProperties properties, Environment environment) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod) {
            return properties.getCors().getProdAllowedOrigins();
        }
        return List.of(properties.getCors().getFrontendOrigin());
    }
}
