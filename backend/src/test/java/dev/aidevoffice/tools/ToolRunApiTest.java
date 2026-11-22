package dev.aidevoffice.tools;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:tool-run-api;MODE=PostgreSQL")
@AutoConfigureMockMvc
@AutoConfigureMetrics
@ActiveProfiles("test")
class ToolRunApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void scopesReviewToProjectAndRequiresExactVersion() throws Exception {
        String url = "/api/projects/" + project("Executor A") + "/tool-runs";
        String other = "/api/projects/" + project("Executor B") + "/tool-runs";
        String payload = json.writeValueAsString(Map.of("tool", "NODE_CHECK", "source", "const answer = 42;"));
        var response = mvc.perform(post(url).with(httpBasic("tester", "test-password"))
                        .header("X-Office-Tool-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        var draft = json.readTree(response);
        String id = draft.get("id").asText();

        mvc.perform(get(url + "/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(get(other + "/" + id).with(httpBasic("tester", "test-password")))
                .andExpect(status().isNotFound());
        mvc.perform(post(url + "/" + id + "/approve").with(httpBasic("tester", "test-password"))
                        .header("X-Office-Tool-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("hash", "0".repeat(64)))))
                .andExpect(status().isConflict());
        mvc.perform(post(url + "/" + id + "/approve").with(httpBasic("tester", "test-password"))
                        .header("X-Office-Tool-Request", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("hash", draft.get("hash").asText()))))
                .andExpect(status().isConflict());
        mvc.perform(post(url + "/" + id + "/cancel").with(httpBasic("tester", "test-password"))
                        .header("X-Office-Tool-Request", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get(url + "/" + id).with(httpBasic("tester", "test-password")))
                .andExpect(jsonPath("$.source").value("const answer = 42;"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void approvedDraftCanBeClaimedOnlyOnceAndCancellationWinsOverResult() throws Exception {
        UUID project = UUID.fromString(project("Fila concorrente"));
        var service = new ToolRunService(jdbc, true, "sha256:" + "a".repeat(64));
        var draft = service.create(project, ToolRunService.Tool.NODE_RUN, "console.log(42);", "tester");
        assertThat(service.queued()).noneMatch(run -> run.id().equals(draft.id()));
        var queued = service.approve(project, draft.id(), draft.hash(), "tester");
        assertThat(queued.approvedBy()).isEqualTo("tester");
        assertThat(service.matchesPolicy(queued)).isTrue();
        jdbc.update("UPDATE office.tool_runs SET review_hash=? WHERE id=?", "0".repeat(64), draft.id());
        assertThat(service.matchesPolicy(service.get(project, draft.id()))).isFalse();
        jdbc.update("UPDATE office.tool_runs SET review_hash=? WHERE id=?", draft.hash(), draft.id());
        assertThat(service.approve(project, draft.id(), draft.hash(), "tester").approvedAt())
                .isEqualTo(queued.approvedAt());

        Callable<Boolean> claim = () -> service.claim(queued);
        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var results = threads.invokeAll(java.util.List.of(claim, claim));
            assertThat(java.util.List.of(results.get(0).get(), results.get(1).get()))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(service.cancel(project, draft.id()).status()).isEqualTo("CANCEL_REQUESTED");
        assertThat(service.finish(draft, new DockerToolExecutor.Result("SUCCEEDED", 0, "42", false, null, 25)))
                .isEqualTo("CANCELLED");
        assertThat(service.claim(queued)).isFalse();
    }

    @Test
    void prometheusMetricsRequireAuthentication() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").with(httpBasic("tester", "test-password")))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsSourceThatCanReorderTheApprovalDisplay() throws Exception {
        String url = "/api/projects/" + project("Revisão de código") + "/tool-runs";
        mvc.perform(post(url).with(httpBasic("tester", "test-password"))
                        .header("X-Office-Tool-Request", "1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("tool", "NODE_RUN", "source", "// \u202e texto"))))
                .andExpect(status().isBadRequest());
    }

    private String project(String name) throws Exception {
        var response = mvc.perform(post("/api/projects").with(httpBasic("tester", "test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "description", "Dados sintéticos"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }
}
