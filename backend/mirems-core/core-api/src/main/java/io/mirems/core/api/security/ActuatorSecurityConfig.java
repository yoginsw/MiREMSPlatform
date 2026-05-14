package io.mirems.core.api.security;

import static org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.to;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class ActuatorSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**")
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
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
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
                .build();
    }
}
