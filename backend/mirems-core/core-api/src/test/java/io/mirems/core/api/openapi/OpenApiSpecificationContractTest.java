package io.mirems.core.api.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class OpenApiSpecificationContractTest {
    private static final Path SPEC_PATH = Path.of("..", "..", "..", "docs", "api", "mirems-api.yaml");

    @Test
    void miremsOpenApiSpecUsesOpenApi31AndDefinesCoreDomainOperations() throws IOException {
        assertThat(Files.exists(SPEC_PATH)).as("docs/api/mirems-api.yaml must exist").isTrue();

        Map<String, Object> spec;
        try (InputStream input = Files.newInputStream(SPEC_PATH)) {
            spec = new Yaml().load(input);
        }

        assertThat(spec.get("openapi")).isEqualTo("3.1.0");
        assertThat(spec.get("info")).isInstanceOf(Map.class);
        assertThat(spec.get("components")).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");

        assertThat(paths).containsKeys(
                "/elections",
                "/elections/{electionId}",
                "/elections/{electionId}/publish",
                "/elections/{electionId}/close",
                "/elections/{electionId}/contests",
                "/elections/{electionId}/contests/{contestId}/candidates",
                "/elections/{electionId}/ballots",
                "/elections/{electionId}/ballot-styles",
                "/elections/{electionId}/ballots/{ballotId}/preview",
                "/voters",
                "/voters/{voterId}",
                "/voters/{voterId}/eligibility/{electionId}",
                "/sessions",
                "/sessions/{sessionId}/cast",
                "/sessions/{sessionId}/spoil",
                "/elections/{electionId}/tabulate",
                "/elections/{electionId}/results",
                "/admin/processes",
                "/admin/processes/{processInstanceId}/signal",
                "/admin/processes/{processInstanceId}/audit");

        assertOperation(paths, "/elections", "post", "createElection");
        assertOperation(paths, "/elections", "get", "listElections");
        assertOperation(paths, "/elections/{electionId}/publish", "put", "publishElection");
        assertOperation(paths, "/sessions/{sessionId}/cast", "post", "castVote");
        assertOperation(paths, "/admin/processes/{processInstanceId}/signal", "post", "signalProcessInstance");

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertThat(schemas).containsKeys(
                "ProblemDetail",
                "ElectionRequest",
                "ElectionResponse",
                "ContestRequest",
                "CandidateRequest",
                "BallotRequest",
                "BallotStyleRequest",
                "VoterRegistrationRequest",
                "VotingSessionRequest",
                "VoteCastRequest",
                "TabulationResultResponse",
                "ProcessStatus",
                "ProcessAuditEntry");
    }

    @Test
    void openApiSpecDeclaresRoleBasedSecuritySchemes() throws IOException {
        Map<String, Object> spec;
        try (InputStream input = Files.newInputStream(SPEC_PATH)) {
            spec = new Yaml().load(input);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> securitySchemes = (Map<String, Object>) components.get("securitySchemes");

        assertThat(securitySchemes).containsKey("bearerAuth");
        assertThat(operationSecurity(spec, "/admin/processes", "get")).contains("SYSTEM_ADMIN");
        assertThat(operationSecurity(spec, "/elections", "post")).contains("ELECTION_ADMIN");
        assertThat(operationSecurity(spec, "/sessions/{sessionId}/cast", "post")).contains("VOTER");
    }

    private static void assertOperation(Map<String, Object> paths, String path, String method, String operationId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);
        @SuppressWarnings("unchecked")
        Map<String, Object> operation = (Map<String, Object>) pathItem.get(method);
        assertThat(operation.get("operationId")).isEqualTo(operationId);
        assertThat(operation.get("responses")).isInstanceOf(Map.class);
    }

    private static List<String> operationSecurity(Map<String, Object> spec, String path, String method) {
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        @SuppressWarnings("unchecked")
        Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);
        @SuppressWarnings("unchecked")
        Map<String, Object> operation = (Map<String, Object>) pathItem.get(method);
        @SuppressWarnings("unchecked")
        List<Map<String, List<String>>> security = (List<Map<String, List<String>>>) operation.get("security");
        return security.getFirst().get("bearerAuth");
    }
}
