package io.mirems.core.api.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.mirems.core.api.MiremsCoreApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(
        classes = MiremsCoreApiApplication.class,
        properties = "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
class BaseUrlConfigurationTest {
    @Autowired
    private Environment environment;

    @Test
    void configuresMiremsPlatformAsBackendContextPath() {
        assertThat(environment.getProperty("server.servlet.context-path")).isEqualTo("/miremsplatform");
    }
}
