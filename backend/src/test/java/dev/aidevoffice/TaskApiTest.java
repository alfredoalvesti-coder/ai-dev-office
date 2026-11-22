package dev.aidevoffice;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiTest {
    private static final String USER = "tester";
    private static final String PASSWORD = "test-password";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearData() {
        jdbc.update("delete from office.task_status_history");
        jdbc.update("delete from office.task_dependencies");
        jdbc.update("delete from office.task_acceptance_criteria");
        jdbc.update("delete from office.tasks");
        jdbc.update("delete from office.projects");
    }

    @Test
    void recordsStatusHistoryAndRejectsInvalidTransitions() throws Exception {
        String projectId = createProject("Task lifecycle");
        JsonNode task = createTask(projectId, "Implement screen", "[]");
        String taskId = task.get("id").asText();
        long version = task.get("version").asLong();

        mvc.perform(put("/api/tasks/{id}/status", taskId).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\",\"version\":" + version + "}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/tasks/{id}/status", taskId).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\",\"version\":" + version + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(version + 1));
        mvc.perform(put("/api/tasks/{id}/status", taskId).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\",\"version\":" + (version + 1) + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));
        mvc.perform(get("/api/tasks/{id}/history", taskId).with(httpBasic(USER, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$[0].toStatus").value("TODO"))
                .andExpect(jsonPath("$[1].toStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[2].toStatus").value("DONE"));
    }

    @Test
    void rejectsDependencyCyclesAndStaleVersions() throws Exception {
        String projectId = createProject("Task graph");
        JsonNode first = createTask(projectId, "First", "[]");
        JsonNode second = createTask(projectId, "Second", "[\"" + first.get("id").asText() + "\"]");
        long version = first.get("version").asLong();

        mvc.perform(put("/api/tasks/{id}/status", second.get("id").asText()).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\",\"version\":0}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/tasks/{id}/dependencies", first.get("id").asText()).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dependencyIds\":[\"" + second.get("id").asText() + "\"],\"version\":" + version + "}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/tasks/{id}/status", first.get("id").asText()).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\",\"version\":" + version + "}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/tasks/{id}/status", first.get("id").asText()).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\",\"version\":" + version + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsDependenciesFromAnotherProject() throws Exception {
        String firstProject = createProject("First project");
        String secondProject = createProject("Second project");
        JsonNode firstTask = createTask(firstProject, "First task", "[]");

        mvc.perform(post("/api/projects/{id}/tasks", secondProject).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Second task\",\"acceptanceCriteria\":[],\"dependencyIds\":[\""
                                + firstTask.get("id").asText() + "\"]}"))
                .andExpect(status().isBadRequest());
    }

    private String createProject(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/projects").with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"\"}"))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private JsonNode createTask(String projectId, String title, String dependencies) throws Exception {
        MvcResult result = mvc.perform(post("/api/projects/{id}/tasks", projectId).with(httpBasic(USER, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"description\":\"\","
                                + "\"acceptanceCriteria\":[\"Build works\"],\"dependencyIds\":"
                                + dependencies + "}"))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
