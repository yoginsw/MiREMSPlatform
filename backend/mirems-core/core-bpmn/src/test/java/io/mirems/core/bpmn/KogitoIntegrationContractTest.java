package io.mirems.core.bpmn;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class KogitoIntegrationContractTest {
    private static final Path MODULE = Path.of(".");

    @Test
    void gradleBuildDeclaresKogitoSpringBootStarterForVersionTenTwo() throws Exception {
        String build = Files.readString(MODULE.resolve("build.gradle.kts"));

        assertTrue(build.contains("org.kie.kogito:kogito-spring-boot-bom:10.2.0"));
        assertTrue(build.contains("org.kie.kogito:spring-boot-starters:10.2.0"));
        assertTrue(build.contains("kogito-spring-boot-starter"));
        assertTrue(build.contains("kogito-processes-spring-boot-starter"));
        assertTrue(build.contains("kogito-decisions-spring-boot-starter"));
    }

    @Test
    void applicationYamlDeclaresKogitoRuntimeEndpointsAndJdbcPersistence() throws Exception {
        String yaml = Files.readString(MODULE.resolve("src/main/resources/application.yml"));

        assertTrue(yaml.contains("service:"));
        assertTrue(yaml.contains("url:"));
        assertTrue(yaml.contains("persistence"));
        assertTrue(yaml.contains("type: jdbc"));
        assertTrue(yaml.contains("data-index"));
    }

    @Test
    void pingBpmnIsExecutableAndReturnsPong() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder()
                .parse(MODULE.resolve("src/main/resources/processes/PingProcess.bpmn").toFile());

        String xml = Files.readString(MODULE.resolve("src/main/resources/processes/PingProcess.bpmn"));

        assertTrue(xml.contains("id=\"ping\""));
        assertTrue(xml.contains("isExecutable=\"true\""));
        assertTrue(xml.contains("pong"));
        assertTrue(document.getElementsByTagNameNS("*", "scriptTask").getLength() >= 1);
    }
}
