package io.mirems.core.api.documentation;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.mirems.core.api.MiremsCoreApiApplication;
import io.mirems.core.bpmn.process.KogitoProcessAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = MiremsCoreApiApplication.class,
        properties = {
                "management.endpoint.health.show-details=always",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApiDocumentationAndHealthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KogitoProcessAdapter kogitoProcessAdapter;

    @Test
    void exposesOpenApiDocsAndSwaggerUiInDevProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", not(blankOrNullString())))
                .andExpect(jsonPath("$.info.title", not(blankOrNullString())));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", not(blankOrNullString())));
    }

    @Test
    void actuatorInfoIncludesVersionGitCommitAndBuildTimeForSystemAdmin() throws Exception {
        mockMvc.perform(get("/actuator/info").with(user("admin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.version", equalTo("0.1.0-SNAPSHOT")))
                .andExpect(jsonPath("$.build.version", equalTo("0.1.0-SNAPSHOT")))
                .andExpect(jsonPath("$.build.time", not(blankOrNullString())))
                .andExpect(jsonPath("$.git.commit", not(blankOrNullString())));
    }

    @Test
    void healthEndpointShowsKogitoProcessEngineStatus() throws Exception {
        mockMvc.perform(get("/actuator/health").with(user("admin").roles("SYSTEM_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.kogito.status", equalTo("UP")))
                .andExpect(jsonPath("$.components.kogito.details.engine", equalTo("Kogito")))
                .andExpect(jsonPath("$.components.kogito.details.adapter", notNullValue()));
    }
}
