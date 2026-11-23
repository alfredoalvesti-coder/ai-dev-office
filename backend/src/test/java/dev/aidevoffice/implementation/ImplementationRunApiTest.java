package dev.aidevoffice.implementation;

import tools.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:implementation-api;MODE=PostgreSQL")
@AutoConfigureMockMvc
@AutoConfigureMetrics
@ActiveProfiles("test")
@Import(ImplementationRunApiTest.StubGeneratorConfig.class)
class ImplementationRunApiTest {
    @TempDir static Path workspace;

    @DynamicPropertySource
    static void workspaceRoot(DynamicPropertyRegistry registry) {
        registry.add("app.implementation.workspace-root", () -> workspace.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @TestConfiguration
    static class StubGeneratorConfig {
        @Bean
        @Primary
        ImplementationGenerator stubGenerator() {
            return new ImplementationGenerator() {
                @Override
                public ImplementationProposal generate(String demand, String approvedPlanJson) {
                    return new ImplementationProposal("CRUD mínimo de notas", List.of(
                            new ProposedFile("README.md", "# Notas\nCRUD simples.\n", "Documentação"),
                            new ProposedFile("src/Note.java", "public class Note { String title; }\n", "Modelo")));
                }

                @Override
                public String model() {
                    return "stub-model";
                }
            };
        }
    }

    @Test
    void createsProposalFromApprovedPlanAndAppliesOnApproval() throws Exception {
        UUID project = project("Workspace demo");
        UUID teamRun = approvedTeamRun(project, "Montar CRUD de notas");

        String created = mvc.perform(post("/api/projects/" + project + "/team-runs/" + teamRun + "/implementations")
                        .with(httpBasic("tester", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn().getResponse().getContentAsString();
        UUID implementationId = UUID.fromString(json.readTree(created).get("id").asText());

        awaitStatus(implementationId, "AWAITING_APPROVAL");

        String detail = mvc.perform(get("/api/implementations/" + implementationId)
                        .with(httpBasic("tester", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
                .andExpect(jsonPath("$.artifact_hash").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String hash = json.readTree(detail).get("artifact_hash").asText();

        mvc.perform(post("/api/implementations/" + implementationId + "/decision")
                        .with(httpBasic("tester", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("decision", "APPROVED", "artifactHash", hash))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));

        mvc.perform(get("/api/projects/" + project + "/workspace/files")
                        .with(httpBasic("tester", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());

        assertThat(workspace.resolve(project.toString()).resolve("README.md")).exists();
    }

    private void awaitStatus(UUID id, String expected) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            String status = jdbc.queryForObject("select status from office.implementation_runs where id=?", String.class, id);
            if (expected.equals(status)) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(jdbc.queryForObject("select status from office.implementation_runs where id=?", String.class, id))
                .isEqualTo(expected);
    }

    private UUID project(String name) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("insert into office.projects(id,name,description,created_at,updated_at) values(?,?,?,?,?)",
                id, name, "demo", java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        return id;
    }

    private UUID approvedTeamRun(UUID projectId, String demand) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String artifact = "{\"summary\":\"plano\",\"tasks\":[],\"steps\":[]}";
        jdbc.update("""
                insert into office.team_runs
                (id,project_id,demand,idempotency_key,status,attempt_count,artifact_json,artifact_hash,context_version,created_at,updated_at)
                values (?,?,?,?,'APPROVED',1,?,?,?,?,?)
                """, id, projectId, demand, UUID.randomUUID().toString(), artifact, "a".repeat(64),
                "team-workflow-v1", java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        return id;
    }
}
