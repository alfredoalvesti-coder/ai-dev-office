package dev.aidevoffice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.BeforeEach;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.security.username=tester", "app.security.password=test-password"})
class ManagementApiTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clearProjects() {
        jdbc.update("delete from office.task_status_history");
        jdbc.update("delete from office.task_dependencies");
        jdbc.update("delete from office.task_acceptance_criteria");
        jdbc.update("delete from office.tasks");
        jdbc.update("delete from office.projects");
    }

    @Test
    void managementEndpointsRequireCredentialsAndHealthRemainsPublic() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/projects").with(httpBasic("tester", "wrong-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void createsAndListsProjectsWithStablePagination() throws Exception {
        mvc.perform(post("/api/projects").with(httpBasic("tester", "test-password"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Alpha","description":"First project"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alpha"))
                .andExpect(jsonPath("$.id").exists());

        mvc.perform(get("/api/projects?page=0&size=10").with(httpBasic("tester", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Alpha"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void rejectsBlankProjectName() throws Exception {
        mvc.perform(post("/api/projects").with(httpBasic("tester", "test-password"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPageSizesAboveTheMaximum() throws Exception {
        mvc.perform(get("/api/projects?size=101").with(httpBasic("tester", "test-password")))
                .andExpect(status().isBadRequest());
    }
}
