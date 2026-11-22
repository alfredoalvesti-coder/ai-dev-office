package dev.aidevoffice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoundationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    Flyway flyway;

    @Test
    void healthIsPublicAndDoesNotExposeInfrastructureDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void readinessChecksDatabase() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void flywayHistoryUsesThePublicSchemaExplicitly() {
        assertThat(flyway.getConfiguration().getDefaultSchema()).isEqualToIgnoringCase("public");
    }

    @Test
    void undocumentedRoutesAreDenied() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void openApiContractIsAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs").with(httpBasic("tester", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AI Dev Office API"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"));
    }
}
